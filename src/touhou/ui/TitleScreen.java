package touhou.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.SpriteSheetCatalog;
import touhou.SpriteRenderer;
import touhou.ImageBank;
import touhou.UnlockFlags;
import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class TitleScreen {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_START = 1;
    public static final int ACTION_EXTRA_START = 2;
    public static final int ACTION_PRACTICE = 3;
    public static final int ACTION_HELP = 4;
    public static final int ACTION_SPELL_PRACTICE = 5;
    public static final int ACTION_REPLAY = 6;
    public static final int ACTION_RESULT = 7;
    public static final int ACTION_MUSIC_ROOM = 8;
    public static final int ACTION_OPTION = 9;
    public static final int ACTION_QUIT = 10;
    public static final int ACTION_STORY = 11;

    private int count;
    private int cursor;
    private boolean extraUnlocked;
    private boolean spellPracticeUnlocked;

    public TitleScreen() {
        reset();
    }

    public void reset() {
        count = 0;
        cursor = 0;
        extraUnlocked = UnlockFlags.isExtraUnlocked();
        spellPracticeUnlocked = UnlockFlags.isSpellPracticeUnlocked();
    }

    public void onReturnToTitle() {
        count = 0;
        extraUnlocked = UnlockFlags.isExtraUnlocked();
        spellPracticeUnlocked = UnlockFlags.isSpellPracticeUnlocked();
        if ((!extraUnlocked && cursor == 1) || (!spellPracticeUnlocked && cursor == 2)) {
            cursor = nextCursor(cursor, 1);
        }
    }

    public int update(int pressed) {
        if (count < 8) {
            count++;
        }

        if (UiInput.cancel(pressed)) {
            return ACTION_HELP;
        }

        if (UiInput.secondary(pressed)) {
            return ACTION_STORY;
        }

        if (UiInput.down(pressed)) {
            cursor = nextCursor(cursor, 1);
        } else if (UiInput.up(pressed)) {
            cursor = nextCursor(cursor, -1);
        } else if (UiInput.confirm(pressed)) {
            if (cursor == 0) {
                return ACTION_START;
            }
            if (cursor == 1 && extraUnlocked) {
                return ACTION_EXTRA_START;
            }
            if (cursor == 2 && spellPracticeUnlocked) {
                return ACTION_SPELL_PRACTICE;
            }
            if (cursor == 3) {
                return ACTION_PRACTICE;
            }
            if (cursor == 4) {
                return ACTION_REPLAY;
            }
            if (cursor == 5) {
                return ACTION_RESULT;
            }
            if (cursor == 6) {
                return ACTION_MUSIC_ROOM;
            }
            if (cursor == 7) {
                return ACTION_OPTION;
            }
            if (cursor == 8) {
                return ACTION_QUIT;
            }
        }

        return ACTION_NONE;
    }

    private int nextCursor(int cur, int delta) {
        int next = cur;
        for (int i = 0; i < 9; i++) {
            next = MenuCursor.move(next, delta, 9);
            if (!extraUnlocked && next == 1) {
                continue;
            }
            if (!spellPracticeUnlocked && next == 2) {
                continue;
            }
            return next;
        }
        return cur;
    }

    public void render(Graphics g, ImageBank imgs, SpriteRenderer sprites) {
        if (imgs == null) {
            return;
        }

        UiDraw.drawRegion(g, imgs.get(0), 0, 0, 0, 0, 240, 240);

        int wob = 4 - (count >> 1);
        if (sprites != null) {
            sprites.draw(g, SpriteSheetCatalog.TITLE_GLYPH_BASE + 0, 30 - wob + 0, 33 - wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_GLYPH_BASE + 1, 30 + wob + 36, 33 + wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_GLYPH_BASE + 2, 30 - wob + 72, 33 - wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_GLYPH_BASE + 3, 30 + wob + 108, 33 + wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_GLYPH_BASE + 4, 30 - wob + 144, 33 - wob);

            sprites.draw(g, SpriteSheetCatalog.TITLE_FRAME, 30 + wob + 0, 33 - wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_FRAME, 30 - wob + 36, 33 + wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_FRAME, 30 + wob + 72, 33 - wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_FRAME, 30 - wob + 108, 33 + wob);
            sprites.draw(g, SpriteSheetCatalog.TITLE_FRAME, 30 + wob + 144, 33 - wob);

            if (count == 8) {
                sprites.draw(g, SpriteSheetCatalog.TITLE_CAPTION, 59, 73);
            }
        }

        Image charImg = imgs.get(2);
        if (charImg != null) {
            g.drawImage(charImg, 5, 93, Graphics.TOP | Graphics.LEFT);
        }

        int y = 104;
        int shown = 0;
        for (int i = 0; i < 9; i++) {
            int id = 766 + i;
            if (i == cursor) {
                id = 775 + i;
            }

            int x;
            if ((shown & 1) == 0) {
                x = 143 - shown * 3 + wob + 8;
            } else {
                x = 143 - shown * 3 - wob + 8;
            }
            shown++;

            if (sprites != null) {
                sprites.draw(g, id, x, y);
            }
            y += 12;
        }

        UiDraw.drawString2(g, null, UiText.get(TextId.TITLE_DESC_0 + cursor), 120, 233, 1, 0x8F7FFF, 0x2F4F7F);
    }
}
