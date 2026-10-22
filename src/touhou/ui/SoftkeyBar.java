package touhou.ui;

import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.i18n.UiText;

/** Device-screen overlay; independent of the logical game viewport and scene images. */
public final class SoftkeyBar {
    private final Image background;

    public SoftkeyBar() throws IOException {
        background = Image.createImage("/sp/128.png");
    }

    public void render(Graphics g, int screenWidth, int screenHeight, int leftTextId, int rightTextId) {
        int y = screenHeight - background.getHeight();
        if (leftTextId >= 0) draw(g, 0, y, leftTextId);
        if (rightTextId >= 0) draw(g, screenWidth - background.getWidth(), y, rightTextId);
    }

    private void draw(Graphics g, int x, int y, int textId) {
        g.drawImage(background, x, y, Graphics.TOP | Graphics.LEFT);
        int baseline = y + (background.getHeight() - UiDraw.fontHeight(null)) / 2 + UiDraw.fontBaseline(null);
        UiDraw.drawStringPlain(g, null, UiText.get(textId), x + background.getWidth() / 2, baseline, 1, 0xF3F1FF);
    }
}
