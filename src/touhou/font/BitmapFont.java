package touhou.font;

import javax.microedition.lcdui.Graphics;

import touhou.i18n.I18n;
import touhou.ResourceException;

public final class BitmapFont {
    private static final int FORMAT_VERSION = 2;
    private static final int BITS_PER_PIXEL = 1;
    private static final int HEADER_SIZE = 14;
    private static BitmapFont instance;

    private final int cellW;
    private final int cellH;
    private final int baseline;
    private final int[] codepoints;
    private final byte[] glyphBits;
    private final byte[] glyphAdvance;
    private final int glyphSizeBytes;
    private final int[] glyphScratch;
    // Bit 0 is foreground, bit 1 is outline; reused across a whole text run.
    private final byte[] outlineMask;

    private BitmapFont(int cellW, int cellH, int baseline, int[] codepoints, byte[] glyphBits, byte[] glyphAdvance) {
        this.cellW = cellW;
        this.cellH = cellH;
        this.baseline = baseline;
        this.codepoints = codepoints;
        this.glyphBits = glyphBits;
        this.glyphAdvance = glyphAdvance;
        this.glyphSizeBytes = ((cellW + 7) >> 3) * cellH;
        int scratchSize = (cellW + 2) * (cellH + 2);
        this.glyphScratch = new int[scratchSize];
        this.outlineMask = new byte[scratchSize];
    }

    public static BitmapFont get() {
        return instance;
    }

    public static void loadForCurrentLanguage() {
        instance = parse(I18n.loadRequiredBytes("font/font.dat"));
    }

    public int getCellH() {
        return cellH;
    }

    public int getBaseline() {
        return baseline;
    }

    public int stringWidth(String text) {
        if (text == null) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += charWidth(text.charAt(i));
        }
        return width;
    }

    public int charWidth(char value) {
        if (value == '\n' || value == '\r') {
            return 0;
        }
        int glyph = findGlyph(value);
        if (glyphAdvance != null && glyph >= 0) {
            int advance = glyphAdvance[glyph] & 0xFF;
            if (advance > 0) {
                return advance;
            }
        }
        return cellW;
    }

    public void drawString(Graphics graphics, String text, int x, int baselineY, int rgb) {
        if (graphics == null || text == null) {
            return;
        }
        int drawX = x;
        int drawY = baselineY - baseline;
        int color = rgb & 0x00FFFFFF;
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (value == '\n' || value == '\r') {
                continue;
            }
            int glyph = findGlyph(value);
            if (value != ' ' && glyph >= 0) {
                drawGlyph(graphics, glyph, drawX, drawY, color);
            }
            drawX += advanceFor(glyph);
        }
    }

    public void drawStringShadow(Graphics graphics, String text, int x, int baselineY, int rgbMain, int rgbShadow) {
        drawString(graphics, text, x + 1, baselineY + 1, rgbShadow);
        drawString(graphics, text, x, baselineY, rgbMain);
    }

    public void drawStringOutline(Graphics graphics, String text, int x, int baselineY, int rgbMain, int rgbOutline) {
        if (graphics == null || text == null) {
            return;
        }
        byte[] mask = outlineMask;
        for (int i = 0; i < mask.length; i++) mask[i] = 0;
        int drawX = x;
        int previousX = x;
        int drawY = baselineY - baseline - 1;
        int main = 0xFF000000 | rgbMain;
        int outline = 0xFF000000 | rgbOutline;
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (value == '\n' || value == '\r') continue;
            int glyph = findGlyph(value);
            if (value != ' ' && glyph >= 0) {
                shiftOutlineMask(drawX - previousX);
                drawOutlinedGlyph(graphics, glyph, drawX - 1, drawY, main, outline);
                previousX = drawX;
            }
            drawX += advanceFor(glyph);
        }
    }

    private int advanceFor(int glyph) {
        if (glyphAdvance != null && glyph >= 0) {
            int advance = glyphAdvance[glyph] & 0xFF;
            if (advance > 0) {
                return advance;
            }
        }
        return cellW;
    }

    private int findGlyph(char value) {
        int codepoint = value;
        int low = 0;
        int high = codepoints.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int candidate = codepoints[middle];
            if (candidate == codepoint) {
                return middle;
            }
            if (candidate < codepoint) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
    }

    private void drawGlyph(Graphics graphics, int glyphIndex, int x, int y, int rgb) {
        byte[] source = glyphBits;
        int[] pixels = glyphScratch;
        int color = 0xFF000000 | rgb;
        int position = glyphIndex * glyphSizeBytes;
        int output = 0;
        int fullBytes = cellW >> 3;
        int remaining = cellW & 7;
        for (int yy = 0; yy < cellH; yy++) {
            for (int b = 0; b < fullBytes; b++) {
                int bits = source[position++] & 0xFF;
                pixels[output++] = (bits & 0x80) != 0 ? color : 0;
                pixels[output++] = (bits & 0x40) != 0 ? color : 0;
                pixels[output++] = (bits & 0x20) != 0 ? color : 0;
                pixels[output++] = (bits & 0x10) != 0 ? color : 0;
                pixels[output++] = (bits & 0x08) != 0 ? color : 0;
                pixels[output++] = (bits & 0x04) != 0 ? color : 0;
                pixels[output++] = (bits & 0x02) != 0 ? color : 0;
                pixels[output++] = (bits & 0x01) != 0 ? color : 0;
            }
            if (remaining != 0) {
                int bits = source[position++] & 0xFF;
                for (int bit = 0; bit < remaining; bit++) {
                    pixels[output++] = (bits & (0x80 >> bit)) != 0 ? color : 0;
                }
            }
        }
        graphics.drawRGB(pixels, 0, cellW, x, y, cellW, cellH, true);
    }

    private void shiftOutlineMask(int distance) {
        byte[] mask = outlineMask;
        int width = cellW + 2;
        for (int row = 0; row < mask.length; row += width) {
            int output = row;
            int end = row + width;
            int source = row + distance;
            // Keep overlapping foreground strokes; new outlines must not cover them.
            while (source < end) mask[output++] = (byte) (mask[source++] & 1);
            while (output < end) mask[output++] = 0;
        }
    }

    private void drawOutlinedGlyph(Graphics graphics, int glyphIndex, int x, int y, int main, int outline) {
        byte[] source = glyphBits;
        byte[] mask = outlineMask;
        int width = cellW + 2;
        int position = glyphIndex * glyphSizeBytes;
        for (int yy = 0; yy < cellH; yy++) {
            int output = (yy + 1) * width + 1;
            for (int xx = 0; xx < cellW;) {
                int bits = source[position++] & 0xFF;
                int count = Math.min(8, cellW - xx);
                for (int bit = 0; bit < count; bit++, output++) {
                    if ((bits & (0x80 >> bit)) == 0) continue;
                    mask[output] = 1;
                    mask[output - width - 1] |= 2;
                    mask[output - width] |= 2;
                    mask[output - width + 1] |= 2;
                    mask[output - 1] |= 2;
                    mask[output + 1] |= 2;
                    mask[output + width - 1] |= 2;
                    mask[output + width] |= 2;
                    mask[output + width + 1] |= 2;
                }
                xx += count;
            }
        }
        int[] pixels = glyphScratch;
        for (int i = 0; i < mask.length; i++) {
            int value = mask[i];
            pixels[i] = (value & 1) != 0 ? main : value != 0 ? outline : 0;
        }
        graphics.drawRGB(pixels, 0, width, x, y, width, cellH + 2, true);
    }

    private static BitmapFont parse(byte[] bytes) {
        if (bytes == null || bytes.length < HEADER_SIZE) {
            throw invalidFont();
        }
        int position = 0;
        if (bytes[position++] != 'B' || bytes[position++] != 'F' || bytes[position++] != 'N' || bytes[position++] != 'T') {
            throw invalidFont();
        }
        int version = bytes[position++] & 0xFF;
        if (version != FORMAT_VERSION) {
            throw invalidFont();
        }
        if ((bytes[position++] & 0xFF) != BITS_PER_PIXEL) {
            throw invalidFont();
        }
        int cellW = bytes[position++] & 0xFF;
        int cellH = bytes[position++] & 0xFF;
        int baseline = bytes[position++] & 0xFF;
        position += 3;
        int glyphCount = readU16(bytes, position);
        position += 2;
        if (cellW <= 0 || cellH <= 0 || baseline >= cellH || glyphCount <= 0) {
            throw invalidFont();
        }

        int tableLength = glyphCount * 4;
        if (position > bytes.length - tableLength) {
            throw invalidFont();
        }
        int[] codepoints = new int[glyphCount];
        int previous = -1;
        for (int i = 0; i < glyphCount; i++) {
            int codepoint = readI32(bytes, position);
            position += 4;
            if (codepoint <= previous) {
                throw invalidFont();
            }
            codepoints[i] = codepoint;
            previous = codepoint;
        }

        if (position > bytes.length - glyphCount) {
            throw invalidFont();
        }
        byte[] advances = new byte[glyphCount];
        System.arraycopy(bytes, position, advances, 0, glyphCount);
        position += glyphCount;

        int glyphBytes = glyphCount * (((cellW + 7) >> 3) * cellH);
        if (glyphBytes < 0 || position != bytes.length - glyphBytes) {
            throw invalidFont();
        }
        byte[] bits = new byte[glyphBytes];
        System.arraycopy(bytes, position, bits, 0, glyphBytes);
        return new BitmapFont(cellW, cellH, baseline, codepoints, bits, advances);
    }

    private static int readU16(byte[] bytes, int position) {
        return ((bytes[position] & 0xFF) << 8) | (bytes[position + 1] & 0xFF);
    }

    private static int readI32(byte[] bytes, int position) {
        return ((bytes[position] & 0xFF) << 24) | ((bytes[position + 1] & 0xFF) << 16)
                | ((bytes[position + 2] & 0xFF) << 8) | (bytes[position + 3] & 0xFF);
    }

    private static ResourceException invalidFont() {
        return new ResourceException("Invalid localized bitmap font");
    }
}
