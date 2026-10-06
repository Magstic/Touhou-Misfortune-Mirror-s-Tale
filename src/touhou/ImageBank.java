package touhou;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public final class ImageBank {
    private static final int[][] STAGEEX_TD = new int[][] {
            { 0x0E7625, 0x109325, 0x104C26, 0x0EB71B, 0x12A627, 0x0E8323, 0x16CE2A, 0x0C6322, 0x17EC28, 0x13D920, 0x10F90C, 0x22C44E, 0x26D356,
                    0x0A4621, 0x083F1D, 0x073C1B },
            { 0x600E76, 0x6F1093, 0x48104C, 0x7A0EB7, 0x7B12A6, 0x650E83, 0x9216CE, 0x530C63, 0xA017EC, 0x9013D9, 0x8F0CF9, 0xA922C4, 0xB826D3,
                    0x430A46, 0x3C083F, 0x39073C },
            { 0x766C0E, 0x937F10, 0x494C10, 0xB78E0E, 0xA68D12, 0x83730E, 0xCEA816, 0x635D0C, 0xECB917, 0xD9A713, 0xF9AA0C, 0xC4BC22, 0xD3CC26,
                    0x42460A, 0x3B3F08, 0x383C07 },
            { 0x0D5428, 0x106B2E, 0x0CFD0B, 0x158237, 0x19F62F, 0x18D033, 0x0B8A1A, 0x2CCD6A, 0x1BB041, 0x2BE664, 0x27B460, 0x1FEA43, 0x0EAA1D,
                    0x1F9C4E, 0x1FCA48, 0x073C1C },
            { 0x540D35, 0x6B1049, 0xFD0BF2, 0x82155A, 0xF619D4, 0xD018AB, 0x8A0B75, 0xCD2C87, 0xB01B82, 0xE62BA4, 0xB42774, 0xEA1FBC, 0xAA0E93,
                    0x9C1F66, 0xCA1F99, 0x3C0724 },
            { 0x39540D, 0x4D6B10, 0xFDFB0B, 0x608215, 0xDFF619, 0xB4D018, 0x7B8A0B, 0x8ECD2C, 0x89B01B, 0xACE62B, 0x7AB427, 0xC6EA1F, 0x9AAA0E,
                    0x6C9C1F, 0xA1CA1F, 0x273C07 },
    };

    private final Image[] images;

    private final Image[] alphaImageCacheImg;
    private final int[] alphaImageCacheImgIndex;
    private final int[] alphaImageCacheAlpha;
    private int alphaImageCacheNext;

    private final Image[] alphaKeyImageCacheImg;
    private final int[] alphaKeyImageCacheImgIndex;
    private final int[] alphaKeyImageCacheAlpha;
    private final int[] alphaKeyImageCacheKeyRgb;
    private int alphaKeyImageCacheNext;

    private final Image[] alphaRegionCacheImg;
    private final int[] alphaRegionCacheImgIndex;
    private final int[] alphaRegionCacheX;
    private final int[] alphaRegionCacheY;
    private final int[] alphaRegionCacheW;
    private final int[] alphaRegionCacheH;
    private final int[] alphaRegionCacheAlpha;
    private int alphaRegionCacheNext;

    private long lastRecoverAt;

    // Perf: alpha cache statistics (per instance).
    private int perfAlphaImageReq;
    private int perfAlphaImageHit;
    private int perfAlphaImageCreate;
    private int perfAlphaRegionReq;
    private int perfAlphaRegionHit;
    private int perfAlphaRegionCreate;
    private int perfAlphaKeyReq;
    private int perfAlphaKeyHit;
    private int perfAlphaKeyCreate;

    public ImageBank() {
        images = new Image[SpriteSheetCatalog.IMAGE_COUNT];

        alphaImageCacheImg = new Image[24];
        alphaImageCacheImgIndex = new int[24];
        alphaImageCacheAlpha = new int[24];
        alphaImageCacheNext = 0;

        alphaKeyImageCacheImg = new Image[12];
        alphaKeyImageCacheImgIndex = new int[12];
        alphaKeyImageCacheAlpha = new int[12];
        alphaKeyImageCacheKeyRgb = new int[12];
        alphaKeyImageCacheNext = 0;

        alphaRegionCacheImg = new Image[32];
        alphaRegionCacheImgIndex = new int[32];
        alphaRegionCacheX = new int[32];
        alphaRegionCacheY = new int[32];
        alphaRegionCacheW = new int[32];
        alphaRegionCacheH = new int[32];
        alphaRegionCacheAlpha = new int[32];
        alphaRegionCacheNext = 0;

    }

    public Image get(int imgIndex) {
        if (imgIndex < 0 || imgIndex >= images.length) {
            return null;
        }
        Image cached = images[imgIndex];
        if (cached != null) {
            return cached;
        }

        // StageEX color variants: generate directly from base, no GIF file needed.
        if (isStageExVariantIndex(imgIndex)) {
            Image variant = buildStageExVariant(imgIndex);
            if (variant != null) {
                images[imgIndex] = variant;
                return variant;
            }
            return null;
        }

        String path = SpriteSheetCatalog.get(imgIndex).getPath();

        Image img = loadByPath(path);
        if (img == null) {
            recoverForImageLoad();
            img = loadByPath(path);
        }
        if (img != null) {
            images[imgIndex] = img;
        }
        return img;
    }

    public boolean draw(Graphics graphics, int imgIndex, int x, int y, int anchor) {
        if (graphics == null) {
            return false;
        }
        Image image = get(imgIndex);
        if (image == null) {
            return false;
        }
        SpriteSheetLayout layout = SpriteSheetCatalog.get(imgIndex);
        int drawX = layout.toPhysicalDestinationX(x);
        int drawY = layout.toPhysicalDestinationY(y);
        if ((anchor & Graphics.RIGHT) != 0) {
            drawX += image.getWidth() - layout.getLogicalWidth();
        } else if ((anchor & Graphics.HCENTER) != 0) {
            drawX += (image.getWidth() - layout.getLogicalWidth()) >> 1;
        }
        if ((anchor & Graphics.BOTTOM) != 0) {
            drawY += image.getHeight() - layout.getLogicalHeight();
        } else if ((anchor & Graphics.VCENTER) != 0) {
            drawY += (image.getHeight() - layout.getLogicalHeight()) >> 1;
        }
        graphics.drawImage(image, drawX, drawY, anchor);
        return true;
    }

    public boolean drawRegion(Graphics graphics, int imgIndex, int srcX, int srcY, int width, int height,
            int transform, int x, int y, int anchor) {
        if (graphics == null || width <= 0 || height <= 0) {
            return false;
        }
        Image image = get(imgIndex);
        if (image == null) {
            return false;
        }
        SpriteSheetLayout layout = SpriteSheetCatalog.get(imgIndex);
        int physicalX = layout.toPhysicalSourceX(srcX);
        int physicalY = layout.toPhysicalSourceY(srcY);
        if (physicalX < 0 || physicalY < 0 || physicalX + width > image.getWidth() || physicalY + height > image.getHeight()) {
            return false;
        }
        graphics.drawRegion(image, physicalX, physicalY, width, height, transform, x, y, anchor);
        return true;
    }

    public int toPhysicalSourceX(int imgIndex, int logicalX) {
        return SpriteSheetCatalog.get(imgIndex).toPhysicalSourceX(logicalX);
    }

    public int toPhysicalSourceY(int imgIndex, int logicalY) {
        return SpriteSheetCatalog.get(imgIndex).toPhysicalSourceY(logicalY);
    }

    public Image getAlphaImageColorKey(int imgIndex, int alpha, int keyRgb) {
        if (alpha <= 0) {
            return null;
        }
        if (alpha >= 255) {
            alpha = 255;
        }

        perfAlphaKeyReq++;

        Image base = get(imgIndex);
        if (base == null) {
            return null;
        }

        for (int i = 0; i < alphaKeyImageCacheImg.length; i++) {
            Image cached = alphaKeyImageCacheImg[i];
            if (cached == null) {
                continue;
            }
            if (alphaKeyImageCacheImgIndex[i] == imgIndex && alphaKeyImageCacheAlpha[i] == alpha && alphaKeyImageCacheKeyRgb[i] == keyRgb) {
                perfAlphaKeyHit++;
                return cached;
            }
        }

        int w = base.getWidth();
        int h = base.getHeight();
        Image img;
        try {
            int[] rgb = new int[w * h];
            base.getRGB(rgb, 0, w, 0, 0, w, h);
            int key = keyRgb & 0x00FFFFFF;
            for (int i = 0; i < rgb.length; i++) {
                int p = rgb[i];
                int rgb24 = p & 0x00FFFFFF;
                if (rgb24 == key) {
                    rgb[i] = 0;
                    continue;
                }
                int a0 = (p >>> 24) & 0xFF;
                if (a0 == 0) {
                    rgb[i] = 0;
                    continue;
                }
                int r = (rgb24 >>> 16) & 0xFF;
                int g = (rgb24 >>> 8) & 0xFF;
                int b = rgb24 & 0xFF;
                int lum = (r * 30 + g * 59 + b * 11) / 100;
                int inv = 255 - lum;
                int aPix = (inv * a0) / 255;
                int a = (aPix * alpha) / 255;
                rgb[i] = (a << 24);
            }
            img = Image.createRGBImage(rgb, w, h, true);
        } catch (Throwable t) {
            img = null;
        }

        if (img != null) {
            int slot = alphaKeyImageCacheNext;
            alphaKeyImageCacheNext = (alphaKeyImageCacheNext + 1) % alphaKeyImageCacheImg.length;

            alphaKeyImageCacheImg[slot] = img;
            alphaKeyImageCacheImgIndex[slot] = imgIndex;
            alphaKeyImageCacheAlpha[slot] = alpha;
            alphaKeyImageCacheKeyRgb[slot] = keyRgb;

            perfAlphaKeyCreate++;
        }
        return img;
    }

    public Image getAlphaImage(int imgIndex, int alpha) {
        if (alpha <= 0) {
            return null;
        }
        if (alpha >= 255) {
            return get(imgIndex);
        }

        perfAlphaImageReq++;

        Image base = get(imgIndex);
        if (base == null) {
            return null;
        }

        for (int i = 0; i < alphaImageCacheImg.length; i++) {
            Image cached = alphaImageCacheImg[i];
            if (cached == null) {
                continue;
            }
            if (alphaImageCacheImgIndex[i] == imgIndex && alphaImageCacheAlpha[i] == alpha) {
                perfAlphaImageHit++;
                return cached;
            }
        }

        int w = base.getWidth();
        int h = base.getHeight();
        Image img;
        try {
            int[] rgb = new int[w * h];
            base.getRGB(rgb, 0, w, 0, 0, w, h);
            for (int i = 0; i < rgb.length; i++) {
                int p = rgb[i];
                int a0 = (p >>> 24) & 0xFF;
                if (a0 == 0) {
                    rgb[i] = 0;
                    continue;
                }
                int a = (a0 * alpha) / 255;
                rgb[i] = (a << 24) | (p & 0x00FFFFFF);
            }
            img = Image.createRGBImage(rgb, w, h, true);
        } catch (Throwable t) {
            img = null;
        }

        if (img != null) {
            int slot = alphaImageCacheNext;
            alphaImageCacheNext = (alphaImageCacheNext + 1) % alphaImageCacheImg.length;

            alphaImageCacheImg[slot] = img;
            alphaImageCacheImgIndex[slot] = imgIndex;
            alphaImageCacheAlpha[slot] = alpha;

            perfAlphaImageCreate++;
        }
        return img;
    }

    public Image getAlphaRegion(int imgIndex, int srcX, int srcY, int w, int h, int alpha) {
        if (w <= 0 || h <= 0) {
            return null;
        }
        if (alpha <= 0) {
            return null;
        }
        if (alpha > 255) {
            alpha = 255;
        }

        perfAlphaRegionReq++;

        Image base = get(imgIndex);
        if (base == null) {
            return null;
        }

        SpriteSheetLayout layout = SpriteSheetCatalog.get(imgIndex);
        int physicalX = layout.toPhysicalSourceX(srcX);
        int physicalY = layout.toPhysicalSourceY(srcY);
        if (physicalX < 0 || physicalY < 0) {
            return null;
        }
        int bw = base.getWidth();
        int bh = base.getHeight();
        if (physicalX >= bw || physicalY >= bh) {
            return null;
        }
        if (physicalX + w > bw || physicalY + h > bh) {
            return null;
        }

        for (int i = 0; i < alphaRegionCacheImg.length; i++) {
            Image cached = alphaRegionCacheImg[i];
            if (cached == null) {
                continue;
            }
            if (alphaRegionCacheImgIndex[i] == imgIndex && alphaRegionCacheX[i] == srcX && alphaRegionCacheY[i] == srcY && alphaRegionCacheW[i] == w
                    && alphaRegionCacheH[i] == h && alphaRegionCacheAlpha[i] == alpha) {
                perfAlphaRegionHit++;
                return cached;
            }
        }

        Image img;
        try {
            int[] rgb = new int[w * h];
            base.getRGB(rgb, 0, w, physicalX, physicalY, w, h);
            for (int i = 0; i < rgb.length; i++) {
                int p = rgb[i];
                int a0 = (p >>> 24) & 0xFF;
                if (a0 == 0) {
                    rgb[i] = 0;
                    continue;
                }
                int a = (a0 * alpha) / 255;
                rgb[i] = (a << 24) | (p & 0x00FFFFFF);
            }
            img = Image.createRGBImage(rgb, w, h, true);
        } catch (Throwable t) {
            img = null;
        }

        if (img != null) {
            int slot = alphaRegionCacheNext;
            alphaRegionCacheNext = (alphaRegionCacheNext + 1) % alphaRegionCacheImg.length;

            alphaRegionCacheImg[slot] = img;
            alphaRegionCacheImgIndex[slot] = imgIndex;
            alphaRegionCacheX[slot] = srcX;
            alphaRegionCacheY[slot] = srcY;
            alphaRegionCacheW[slot] = w;
            alphaRegionCacheH[slot] = h;
            alphaRegionCacheAlpha[slot] = alpha;

            perfAlphaRegionCreate++;
        }
        return img;
    }

    // Perf counters are sampled by Debug overlay.
    public int consumePerfAlphaImageReq() {
        int v = perfAlphaImageReq;
        perfAlphaImageReq = 0;
        return v;
    }

    public int consumePerfAlphaImageHit() {
        int v = perfAlphaImageHit;
        perfAlphaImageHit = 0;
        return v;
    }

    public int consumePerfAlphaImageCreate() {
        int v = perfAlphaImageCreate;
        perfAlphaImageCreate = 0;
        return v;
    }

    public int consumePerfAlphaRegionReq() {
        int v = perfAlphaRegionReq;
        perfAlphaRegionReq = 0;
        return v;
    }

    public int consumePerfAlphaRegionHit() {
        int v = perfAlphaRegionHit;
        perfAlphaRegionHit = 0;
        return v;
    }

    public int consumePerfAlphaRegionCreate() {
        int v = perfAlphaRegionCreate;
        perfAlphaRegionCreate = 0;
        return v;
    }

    public int consumePerfAlphaKeyReq() {
        int v = perfAlphaKeyReq;
        perfAlphaKeyReq = 0;
        return v;
    }

    public int consumePerfAlphaKeyHit() {
        int v = perfAlphaKeyHit;
        perfAlphaKeyHit = 0;
        return v;
    }

    public int consumePerfAlphaKeyCreate() {
        int v = perfAlphaKeyCreate;
        perfAlphaKeyCreate = 0;
        return v;
    }

    // Clear all alpha caches to free memory. Does NOT call System.gc().
    public void clearAlphaCaches() {
        for (int i = 0; i < alphaImageCacheImg.length; i++) {
            alphaImageCacheImg[i] = null;
        }
        for (int i = 0; i < alphaKeyImageCacheImg.length; i++) {
            alphaKeyImageCacheImg[i] = null;
        }
        for (int i = 0; i < alphaRegionCacheImg.length; i++) {
            alphaRegionCacheImg[i] = null;
        }
        alphaImageCacheNext = 0;
        alphaKeyImageCacheNext = 0;
        alphaRegionCacheNext = 0;
    }

    private void recoverForImageLoad() {
        long now = System.currentTimeMillis();
        if (now - lastRecoverAt < 1000) {
            return;
        }
        lastRecoverAt = now;

        for (int i = 0; i < alphaImageCacheImg.length; i++) {
            alphaImageCacheImg[i] = null;
        }
        for (int i = 0; i < alphaKeyImageCacheImg.length; i++) {
            alphaKeyImageCacheImg[i] = null;
        }
        for (int i = 0; i < alphaRegionCacheImg.length; i++) {
            alphaRegionCacheImg[i] = null;
        }
        alphaImageCacheNext = 0;
        alphaKeyImageCacheNext = 0;
        alphaRegionCacheNext = 0;

        System.gc();
    }

    private static Image loadByPath(String path) {
        return tryLoad(path);
    }

    private static Image tryLoad(String path) {
        return ResourceLoader.loadOptionalImage(path);
    }

    private static boolean isStageExVariantIndex(int imgIndex) {
        return (imgIndex >= 78 && imgIndex <= 80) || (imgIndex >= 82 && imgIndex <= 84);
    }

    private Image buildStageExVariant(int imgIndex) {
        if (imgIndex >= 78 && imgIndex <= 80) {
            return buildStageExVariantFromBase(77, imgIndex - 78);
        }
        if (imgIndex >= 82 && imgIndex <= 84) {
            return buildStageExVariantFromBase(81, (imgIndex - 82) + 3);
        }
        return null;
    }

    private Image buildStageExVariantFromBase(int baseImgIndex, int tdIndex) {
        if (tdIndex < 0 || tdIndex >= STAGEEX_TD.length) {
            return null;
        }

        String basePath = SpriteSheetCatalog.get(baseImgIndex).getPath();
        byte[] base = ResourceLoader.readOptionalBytes(basePath);
        if (base == null) {
            return null;
        }

        byte[] modified;
        try {
            modified = new byte[base.length];
            System.arraycopy(base, 0, modified, 0, base.length);
        } catch (Throwable t) {
            return null;
        }

        applyGifGlobalPalette(modified, STAGEEX_TD[tdIndex], 0);

        try {
            return Image.createImage(modified, 0, modified.length);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void applyGifGlobalPalette(byte[] gifBytes, int[] palette, int paletteIndex) {
        if (gifBytes == null || palette == null) {
            return;
        }
        int offset = paletteIndex * 3 + 13;
        int need = offset + palette.length * 3;
        if (need > gifBytes.length) {
            return;
        }
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            gifBytes[offset + 0] = (byte) ((c >> 16) & 0xFF);
            gifBytes[offset + 1] = (byte) ((c >> 8) & 0xFF);
            gifBytes[offset + 2] = (byte) (c & 0xFF);
            offset += 3;
        }
    }

}
