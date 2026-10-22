package touhou.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.ImageBank;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
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

    private static final int ITEM_START = 0;
    private static final int ITEM_EXTRA = 1;
    private static final int ITEM_SPELL_PRACTICE = 2;
    private static final int ITEM_STAGE_PRACTICE = 3;
    private static final int ITEM_REPLAY = 4;
    private static final int ITEM_RESULTS = 5;
    private static final int ITEM_MUSIC = 6;
    private static final int ITEM_OPTIONS = 7;
    private static final int ITEM_QUIT = 8;
    private static final int ITEM_COUNT = 9;

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
        if ((!extraUnlocked && cursor == ITEM_EXTRA) || (!spellPracticeUnlocked && cursor == ITEM_SPELL_PRACTICE)) {
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
            if (cursor == ITEM_START) {
                return ACTION_START;
            }
            if (cursor == ITEM_EXTRA && extraUnlocked) {
                return ACTION_EXTRA_START;
            }
            if (cursor == ITEM_SPELL_PRACTICE && spellPracticeUnlocked) {
                return ACTION_SPELL_PRACTICE;
            }
            if (cursor == ITEM_STAGE_PRACTICE) {
                return ACTION_PRACTICE;
            }
            if (cursor == ITEM_REPLAY) {
                return ACTION_REPLAY;
            }
            if (cursor == ITEM_RESULTS) {
                return ACTION_RESULT;
            }
            if (cursor == ITEM_MUSIC) {
                return ACTION_MUSIC_ROOM;
            }
            if (cursor == ITEM_OPTIONS) {
                return ACTION_OPTION;
            }
            if (cursor == ITEM_QUIT) {
                return ACTION_QUIT;
            }
        }

        return ACTION_NONE;
    }

    private int nextCursor(int cur, int delta) {
        int next = cur;
        for (int i = 0; i < ITEM_COUNT; i++) {
            next = MenuCursor.move(next, delta, ITEM_COUNT);
            if (!extraUnlocked && next == ITEM_EXTRA) {
                continue;
            }
            if (!spellPracticeUnlocked && next == ITEM_SPELL_PRACTICE) {
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

        UiDraw.drawRegion(g, imgs.get(SpriteSheetCatalog.TITLE_BACKGROUND_IMAGE), 0, 0, 0, 0, 240, 240);

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

        Image charImg = imgs.get(SpriteSheetCatalog.TITLE_PORTRAIT_IMAGE);
        if (charImg != null) {
            g.drawImage(charImg, 5, 93, Graphics.TOP | Graphics.LEFT);
        }

        int y = 104;
        int shown = 0;
        for (int i = 0; i < ITEM_COUNT; i++) {
            int id = SpriteSheetCatalog.TITLE_MENU_BASE + i;
            if (i == cursor) {
                id = SpriteSheetCatalog.TITLE_MENU_SELECTED_BASE + i;
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
