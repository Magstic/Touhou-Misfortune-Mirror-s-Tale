package touhou;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

/** All atlas sampling, trim positioning and sprite-local clipping pass through here. */
public final class SpriteRenderer {
    private final SpriteCatalog catalog;
    private final ImageBank images;

    public SpriteRenderer(SpriteCatalog catalog, ImageBank images) {
        this.catalog = catalog;
        this.images = images;
    }
    public ImageBank getImages() { return images; }

    public boolean getSizeAndAnchor(int id, int[] out) {
        if (!catalog.hasSpriteMeta(id) || out == null || out.length < 4) return false;
        out[0] = catalog.getW(id); out[1] = catalog.getH(id);
        out[2] = catalog.getAx(id); out[3] = catalog.getAy(id);
        return true;
    }
    public int getImageIndex(int id) { return catalog.getImgIndex(id); }

    public void precacheAlphaRegion(int id, int alpha) {
        requireSprite(id);
        if (catalog.renderW(id) > 0 && alpha > 0 && alpha < 255) {
            images.getAlphaRegion(catalog.getImgIndex(id), catalog.pixelX(id), catalog.pixelY(id),
                    catalog.renderW(id), catalog.renderH(id), alpha);
        }
    }
    public void draw(Graphics g, int id, int x, int y) {
        drawAlpha(g, id, x, y, 255);
    }
    public void drawAlpha(Graphics g, int id, int x, int y, int alpha) {
        requireSprite(id);
        paint(g, id, x, y, alpha, 0, 0, catalog.getW(id), catalog.getH(id), false,
                g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
    }
    public void drawFast(Graphics g, int id, int x, int y, int alpha,
            int clipX, int clipY, int clipW, int clipH) {
        requireSprite(id);
        paint(g, id, x, y, alpha, 0, 0, catalog.getW(id), catalog.getH(id), true,
                clipX, clipY, clipW, clipH);
    }
    public void drawAlphaCropRight(Graphics g, int id, int x, int y, int alpha, int cutRight) {
        requireSprite(id);
        drawWindow(g, id, x, y, 0, 0, catalog.getW(id) - Math.max(0, cutRight), catalog.getH(id), alpha);
    }
    // Window coordinates are local to the original logical sprite canvas. x/y are its anchor position.
    public void drawWindow(Graphics g, int id, int x, int y, int left, int top, int w, int h, int alpha) {
        requireSprite(id);
        paint(g, id, x, y, alpha, left, top, w, h, false,
                g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
    }

    // The catalog keeps the full logical mask size; its pixel region stores only the upper-left quarter.
    // The table describes quarter assembly and any transparent center-row gap.
    public void drawMirroredColorKey(Graphics g, int id, int x, int y, int alpha, int keyRgb) {
        requireSprite(id);
        if (alpha <= 0 || g.getClipWidth() <= 0 || g.getClipHeight() <= 0) return;
        if (catalog.renderMode(id) != SpriteCatalog.QUARTER) throw new IllegalArgumentException("Not a quarter sprite: " + id);
        int bottomGap = catalog.bottomGap(id);
        int w = catalog.renderW(id), h = catalog.renderH(id);
        if (w == 0) return;
        Image image = images.getAlphaImageColorKey(catalog.getImgIndex(id), Math.min(255, alpha), keyRgb);
        if (image == null) throw new ResourceException("Cannot create sprite color-key image: " + id);
        int sx = catalog.pixelX(id), sy = catalog.pixelY(id);
        x -= catalog.getAx(id); y -= catalog.getAy(id);
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        try {
            g.clipRect(x, y, catalog.getW(id), catalog.getH(id));
            g.drawRegion(image, sx, sy, w, h, Sprite.TRANS_NONE, x, y, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(image, sx, sy, w, h, Sprite.TRANS_MIRROR, x + w, y, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(image, sx, sy, w, h, Sprite.TRANS_MIRROR_ROT180,
                    x, y + h + bottomGap, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(image, sx, sy, w, h, Sprite.TRANS_ROT180,
                    x + w, y + h + bottomGap, Graphics.TOP | Graphics.LEFT);
        } finally {
            g.setClip(cx, cy, cw, ch);
        }
    }

    private void paint(Graphics g, int id, int x, int y, int alpha,
            int left, int top, int w, int h, boolean fast, int cx, int cy, int cw, int ch) {
        if (alpha <= 0 || w <= 0 || h <= 0 || cw <= 0 || ch <= 0 || catalog.renderW(id) == 0) return;
        if (alpha > 255) alpha = 255;
        if (catalog.renderMode(id) == SpriteCatalog.QUARTER || catalog.renderMode(id) == SpriteCatalog.HALF) {
            paintMirrored(g, id, x, y, alpha, left, top, w, h, fast, cx, cy, cw, ch);
            return;
        }
        int tx = catalog.trimX(id), ty = catalog.trimY(id);
        int x0 = Math.max(tx, Math.max(0, left));
        int y0 = Math.max(ty, Math.max(0, top));
        int x1 = Math.min(tx + catalog.renderW(id), Math.min(catalog.getW(id), left + w));
        int y1 = Math.min(ty + catalog.renderH(id), Math.min(catalog.getH(id), top + h));
        int originX = x - catalog.getAx(id), originY = y - catalog.getAy(id);
        x0 = Math.max(x0, cx - originX); y0 = Math.max(y0, cy - originY);
        x1 = Math.min(x1, cx + cw - originX); y1 = Math.min(y1, cy + ch - originY);
        if (x0 >= x1 || y0 >= y1) return;
        int imageId = catalog.getImgIndex(id);
        Image img = images.get(imageId);
        if (img == null) throw new ResourceException("Missing sprite sheet: " + imageId);
        int dw = x1 - x0, dh = y1 - y0;
        boolean mirrored = catalog.renderMode(id) == SpriteCatalog.MIRROR;
        // Reflect the clipped source offset, not just the final draw destination.
        int px = mirrored ? catalog.renderW(id) - (x0 - tx) - dw : x0 - tx;
        int sx = catalog.pixelX(id) + px, sy = catalog.pixelY(id) + y0 - ty;
        if (alpha < 255) {
            // Fast bullets use a full-sheet cache; regular sprites use compact cached regions.
            Image a = fast ? images.getAlphaImage(imageId, alpha)
                    : images.getAlphaRegion(imageId, catalog.pixelX(id), catalog.pixelY(id),
                            catalog.renderW(id), catalog.renderH(id), alpha);
            if (a == null) throw new ResourceException("Cannot create sprite alpha image: " + id);
            img = a;
            if (!fast) { sx = px; sy = y0 - ty; }
        }
        if (fast || mirrored) {
            g.drawRegion(img, sx, sy, dw, dh, mirrored ? Sprite.TRANS_MIRROR : Sprite.TRANS_NONE,
                    originX + x0, originY + y0, Graphics.TOP | Graphics.LEFT);
        } else {
            try {
                g.setClip(originX + x0, originY + y0, dw, dh);
                g.drawImage(img, originX + x0 - sx, originY + y0 - sy, Graphics.TOP | Graphics.LEFT);
            } finally {
                g.setClip(cx, cy, cw, ch);
            }
        }
    }
    // Clip each reflected section in logical coordinates before sampling its atlas region.
    // This also keeps drawFast's contract: no Graphics clip changes and no temporary image assembly.
    private void paintMirrored(Graphics g, int id, int x, int y, int alpha,
            int left, int top, int w, int h, boolean fast, int cx, int cy, int cw, int ch) {
        int originX = x - catalog.getAx(id), originY = y - catalog.getAy(id);
        int x0 = Math.max(0, Math.max(left, cx - originX));
        int y0 = Math.max(0, Math.max(top, cy - originY));
        int x1 = Math.min(catalog.getW(id), Math.min(left + w, cx + cw - originX));
        int y1 = Math.min(catalog.getH(id), Math.min(top + h, cy + ch - originY));
        if (x0 >= x1 || y0 >= y1) return;
        int imageId = catalog.getImgIndex(id), rw = catalog.renderW(id), rh = catalog.renderH(id);
        int sx = catalog.pixelX(id), sy = catalog.pixelY(id);
        Image image;
        if (alpha == 255) image = images.get(imageId);
        else if (fast) image = images.getAlphaImage(imageId, alpha);
        else {
            image = images.getAlphaRegion(imageId, sx, sy, rw, rh, alpha);
            sx = sy = 0;
        }
        if (image == null) throw new ResourceException("Cannot load mirrored sprite: " + id);
        int sections = catalog.renderMode(id) == SpriteCatalog.QUARTER ? 4 : 2;
        int tx = catalog.trimX(id), ty = catalog.trimY(id);
        for (int section = 0; section < sections; section++) {
            boolean mirrorX = (section & 1) != 0, mirrorY = (section & 2) != 0;
            int qx = tx + (mirrorX ? rw : 0), qy = ty + (mirrorY ? rh + catalog.bottomGap(id) : 0);
            int dx = Math.max(x0, qx), dy = Math.max(y0, qy);
            int dw = Math.min(x1, qx + rw) - dx, dh = Math.min(y1, qy + rh) - dy;
            if (dw <= 0 || dh <= 0) continue;
            int px = mirrorX ? rw - (dx - qx) - dw : dx - qx;
            int py = mirrorY ? rh - (dy - qy) - dh : dy - qy;
            int transform = mirrorY ? (mirrorX ? Sprite.TRANS_ROT180 : Sprite.TRANS_MIRROR_ROT180)
                    : (mirrorX ? Sprite.TRANS_MIRROR : Sprite.TRANS_NONE);
            g.drawRegion(image, sx + px, sy + py, dw, dh, transform,
                    originX + dx, originY + dy, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void requireSprite(int id) {
        if (!catalog.hasSpriteMeta(id)) throw new IllegalArgumentException("spriteId=" + id);
    }
}
