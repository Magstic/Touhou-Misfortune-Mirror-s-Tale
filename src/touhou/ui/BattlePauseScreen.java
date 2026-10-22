package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class BattlePauseScreen {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_RESUME = 1;
    public static final int ACTION_QUIT_TO_TITLE = 2;
    public static final int ACTION_RESTART = 3;
    public static final int ACTION_CONTINUE_YES = 4;
    public static final int ACTION_CONTINUE_NO = 5;
    public static final int ACTION_TOGGLE_BGM = 6;

    public static final int MODE_PAUSE = 0;
    public static final int MODE_CONTINUE = 1;

    private int mode;
    private int cursor;
    private int remainingContinues;
    private boolean spellPractice;

    public void enterPause(boolean spellPractice) {
        mode = MODE_PAUSE;
        this.spellPractice = spellPractice;
        remainingContinues = 0;
        cursor = 0;
    }

    public void enterContinue(int remainingContinues) {
        mode = MODE_CONTINUE;
        this.remainingContinues = remainingContinues;
        cursor = 0;
    }

    public int update(int pressed) {
        if (mode == MODE_CONTINUE) {
            return updateContinue(pressed);
        }
        return updatePause(pressed);
    }

    public int getLeftSoftkeyTextId() {
        return mode == MODE_PAUSE ? TextId.SOFTKEY_RESUME : -1;
    }

    public int getRightSoftkeyTextId() {
        return mode == MODE_PAUSE ? TextId.SOFTKEY_BGM : -1;
    }

    private int updatePause(int pressed) {
        if (UiInput.cancel(pressed)) {
            return ACTION_RESUME;
        }
        if (UiInput.secondary(pressed)) {
            return ACTION_TOGGLE_BGM;
        }

        if (UiInput.down(pressed)) {
            cursor = MenuCursor.move(cursor, 1, 3);
            return ACTION_NONE;
        }
        if (UiInput.up(pressed)) {
            cursor = MenuCursor.move(cursor, -1, 3);
            return ACTION_NONE;
        }

        if (UiInput.confirm(pressed)) {
            if (cursor == 0) {
                return ACTION_RESUME;
            }
            if (cursor == 1) {
                return ACTION_QUIT_TO_TITLE;
            }
            return ACTION_RESTART;
        }

        return ACTION_NONE;
    }

    private int updateContinue(int pressed) {
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
                return ACTION_CONTINUE_YES;
            }
            return ACTION_CONTINUE_NO;
        }

        return ACTION_NONE;
    }

    public void render(Graphics g) {
        if (g == null) {
            return;
        }

        if (mode == MODE_CONTINUE) {
            renderContinue(g);
            return;
        }

        renderPause(g);
    }

    private void renderPause(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 70, 240, 100);

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        UiDraw.drawString2(g, font, UiText.get(TextId.BATTLE_PAUSE_TITLE), 120, 90, 1, 0xFF0000, 0x770000);

        String s0 = UiText.get(TextId.BATTLE_PAUSE_RESUME);
        String s1 = spellPractice ? UiText.get(TextId.BATTLE_PAUSE_BACK_TO_MENU) : UiText.get(TextId.BATTLE_PAUSE_BACK_TO_TITLE);
        String s2 = UiText.get(TextId.BATTLE_PAUSE_RESTART);

        UiDraw.drawString2(g, font, s0, 120, 110, 1, 0x777777, 0x333333);
        UiDraw.drawString2(g, font, s1, 120, 130, 1, 0x777777, 0x333333);
        UiDraw.drawString2(g, font, s2, 120, 150, 1, 0x777777, 0x333333);

        if (cursor == 0) {
            UiDraw.drawString2(g, font, s0, 120, 110, 1, 0xFFFFFF, 0x777777);
        } else if (cursor == 1) {
            UiDraw.drawString2(g, font, s1, 120, 130, 1, 0xFFFFFF, 0x777777);
        } else {
            UiDraw.drawString2(g, font, s2, 120, 150, 1, 0xFFFFFF, 0x777777);
        }
    }

    private void renderContinue(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 80, 194, 80);

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        UiDraw.drawString2(g, font, UiText.get(TextId.BATTLE_CONTINUE_TITLE), 97, 100, 1, 0xFF0000, 0x770000);

        String s0 = UiText.get(TextId.BATTLE_CONTINUE_YES);
        String s1 = UiText.get(TextId.BATTLE_CONTINUE_NO);

        UiDraw.drawString2(g, font, s0, 97, 120, 1, 0x777777, 0x333333);
        UiDraw.drawString2(g, font, s1, 97, 140, 1, 0x777777, 0x333333);

        if (cursor == 0) {
            UiDraw.drawString2(g, font, s0, 97, 120, 1, 0xFFFFFF, 0x777777);
        } else {
            UiDraw.drawString2(g, font, s1, 97, 140, 1, 0xFFFFFF, 0x777777);
        }

        String cnt = UiText.get(TextId.BATTLE_CONTINUE_REMAINING_PREFIX) + remainingContinues + UiText.get(TextId.BATTLE_CONTINUE_REMAINING_SUFFIX);
        UiDraw.drawString2(g, font, cnt, 147, 154, 0, 0x770000, 0x330000);
    }
}
