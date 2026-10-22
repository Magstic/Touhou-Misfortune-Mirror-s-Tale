package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class QuitScreen {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_BACK_TO_TITLE = 1;
    public static final int ACTION_QUIT = 2;

    private int cursor;

    public void enter() {
        cursor = 0;
    }

    public int update(int pressed) {
        if (UiInput.cancel(pressed)) {
            return ACTION_BACK_TO_TITLE;
        }

        if (UiInput.down(pressed)) {
            cursor = MenuCursor.move(cursor, 1, 2);
            return ACTION_NONE;
        }

        if (UiInput.up(pressed)) {
            cursor = MenuCursor.move(cursor, -1, 2);
            return ACTION_NONE;
        }

        if (UiInput.confirm(pressed)) {
            if (cursor == 0) {
                return ACTION_BACK_TO_TITLE;
            }
            return ACTION_QUIT;
        }

        return ACTION_NONE;
    }

    public void render(Graphics g) {
        if (g == null) {
            return;
        }

        g.setColor(0x000000);
        g.fillRect(48, 80, 144, 80);
        g.setColor(0xFF0000);
        g.fillRect(49, 81, 142, 78);
        g.setColor(0x000000);
        g.fillRect(50, 82, 140, 76);

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        // I18N
        UiDraw.drawString2(g, font, UiText.get(TextId.QUIT_TITLE), 120, 100, 1, 0xFF0000, 0x770000);

        String s0 = UiText.get(TextId.QUIT_OPTION_CONTINUE);
        String s1 = UiText.get(TextId.QUIT_OPTION_QUIT);

        UiDraw.drawString2(g, font, s0, 120, 120, 1, 0x777777, 0x333333);
        UiDraw.drawString2(g, font, s1, 120, 140, 1, 0x777777, 0x333333);

        if (cursor == 0) {
            UiDraw.drawString2(g, font, s0, 120, 120, 1, 0xFFFFFF, 0x777777);
        } else if (cursor == 1) {
            UiDraw.drawString2(g, font, s1, 120, 140, 1, 0xFFFFFF, 0x777777);
        }
    }
}
