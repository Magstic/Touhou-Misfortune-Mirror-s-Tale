package touhou.font;

import javax.microedition.lcdui.Graphics;

import touhou.i18n.I18n;
import touhou.ResourceException;

public final class BitmapFont {
    private static BitmapFont instance;

    private final int cellW;
    private final int cellH;
    private final int baseline;
    private final int[] codepoints;
    private final byte[] glyphBits;
    private final byte[] glyphAdvance;
    private final int glyphStrideBytes;
    private final int glyphSizeBytes;
    private final int[] glyphScratch;

    private BitmapFont(int cellW, int cellH, int baseline, int[] codepoints, byte[] glyphBits, byte[] glyphAdvance) {
        this.cellW = cellW;
        this.cellH = cellH;
        this.baseline = baseline;
        this.codepoints = codepoints;
        this.glyphBits = glyphBits;
        this.glyphAdvance = glyphAdvance;
        this.glyphStrideBytes = (cellW + 7) >> 3;
        this.glyphSizeBytes = glyphStrideBytes * cellH;
        this.glyphScratch = new int[cellW * cellH];
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
        int base = glyphIndex * glyphSizeBytes;
        int output = 0;
        for (int yy = 0; yy < cellH; yy++) {
            int row = base + yy * glyphStrideBytes;
            for (int xx = 0; xx < cellW; xx++) {
                int bits = glyphBits[row + (xx >> 3)] & 0xFF;
                boolean on = ((bits >> (7 - (xx & 7))) & 1) != 0;
                glyphScratch[output++] = on ? (0xFF000000 | rgb) : 0;
            }
        }
        graphics.drawRGB(glyphScratch, 0, cellW, x, y, cellW, cellH, true);
    }

    private static BitmapFont parse(byte[] bytes) {
        if (bytes == null || bytes.length < 14) {
            throw invalidFont();
        }
        int position = 0;
        if (bytes[position++] != 'B' || bytes[position++] != 'F' || bytes[position++] != 'N' || bytes[position++] != 'T') {
            throw invalidFont();
        }
        int version = bytes[position++] & 0xFF;
        if (version != 2) {
            throw invalidFont();
        }
        if ((bytes[position++] & 0xFF) != 1) {
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
