package touhou;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** EX background: blend palette pairs once instead of doing RGB arithmetic per pixel. */
public final class AdditiveBackground {
    private static final int WIDTH = 194;
    private static final int HEIGHT = 226;
    private final int[] output = new int[WIDTH * HEIGHT];
    private final Sheet base;
    private final Sheet overlay;
    private final int[] colors;
    private int lastRatio = -1;
    private int lastDarkness = -1;

    public AdditiveBackground(Image base, Image overlay) {
        this.base = new Sheet(base, output);
        this.overlay = new Sheet(overlay, output);
        colors = new int[this.base.count * this.overlay.count];
    }

    public void draw(Graphics g, int ratio, int scroll, int originX, int originY, int darkness) {
        int count = overlay.count;
        if (lastRatio != ratio || lastDarkness != darkness) {
            for (int b = 0; b < base.count; b++) {
                int dst = base.colors[b];
                for (int s = 0; s < count; s++) {
                    int src = overlay.colors[s], rgb = dst;
                    if ((src >>> 24) != 0) {
                        int r = (((dst >> 16 & 255) + (src >> 16 & 255)) * ratio) / 255;
                        int green = (((dst >> 8 & 255) + (src >> 8 & 255)) * ratio) / 255;
                        int blue = (((dst & 255) + (src & 255)) * ratio) / 255;
                        rgb = (r << 16) | (green << 8) | blue;
                    }
                    colors[b * count + s] = 0xFF000000 | ImageBank.darkenColor(rgb, darkness);
                }
            }
            lastRatio = ratio;
            lastDarkness = darkness;
        }
        byte[] bg = base.pixels, over = overlay.pixels;
        int[] lookup = colors, out = output;
        int left = Math.max(0, originX);
        int startX = (left - originX) % overlay.width;
        int index = 0;
        for (int y = 0; y < HEIGHT; y++) {
            int b = (8 + y) * base.width;
            int sy = (y - originY - scroll) % overlay.height;
            if (sy < 0) {
                sy += overlay.height;
            }
            int row = sy * overlay.width;
            int x = 0;
            for (; x < left; x++) {
                out[index++] = lookup[(bg[b++] & 255) * count];
            }
            // Wrap at tile boundaries without a modulo in the pixel loop.
            int sx = startX;
            while (x < WIDTH) {
                int end = Math.min(WIDTH, x + overlay.width - sx);
                int s = row + sx;
                for (; x < end; x++) {
                    out[index++] = lookup[(bg[b++] & 255) * count + (over[s++] & 255)];
                }
                sx = 0;
            }
        }
        g.drawRGB(out, 0, WIDTH, 0, 8, WIDTH, HEIGHT, false);
    }

    private static final class Sheet {
        final int width, height;
        final byte[] pixels;
        final int[] colors = new int[256];
        int count = 1;

        Sheet(Image image, int[] scratch) {
            width = image.getWidth();
            height = image.getHeight();
            pixels = new byte[width * height];
            int[] keys = new int[512];
            short[] entries = new short[512];
            int rows = scratch.length / width;
            for (int y = 0; y < height; y += rows) {
                int h = Math.min(rows, height - y);
                image.getRGB(scratch, 0, width, 0, y, width, h);
                for (int i = 0; i < width * h; i++) {
                    int argb = scratch[i];
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                    int hash = (argb ^ (argb >>> 9)) & 511;
                    while (entries[hash] != 0 && keys[hash] != argb) {
                        hash = (hash + 1) & 511;
                    }
                    int color = entries[hash];
                    if (color == 0) {
                        color = count++;
                        keys[hash] = argb;
                        entries[hash] = (short) color;
                        colors[color] = argb;
                    }
                    pixels[y * width + i] = (byte) color;
                }
            }
        }
    }
}
