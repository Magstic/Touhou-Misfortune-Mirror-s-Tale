package touhou.ui;

import javax.microedition.lcdui.Font;

/** Bridges the active MIDP/bitmap font to the pure text layout rules. */
public final class FontTextMeasurer implements UiTextLayout.Measurer {
    private Font font;

    void setFont(Font font) {
        this.font = font;
    }

    public int charWidth(char c) {
        return UiDraw.charWidth(font, c);
    }
}
