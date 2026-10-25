package touhou.ui;

import javax.microedition.lcdui.Graphics;

import touhou.GameOptions;
import touhou.ImageBank;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
import touhou.i18n.I18n;
import touhou.i18n.I18nBootstrap;

public final class StartupLanguageMenu {
    // Logical items retain their original size; selected/unselected frames have stable IDs.
    private static final int ITEM_H = 13;

    // Match TitleScreen menu area (y=104, 9 rows * 12px) and center our smaller list within it.
    private static final int TITLE_MENU_Y0 = 104;
    private static final int TITLE_MENU_STEP = 12;
    private static final int TITLE_MENU_ROWS = 9;

    // Stair layout: make vertical spacing a bit larger than the baked 13+2=15px stride.
    private static final int ITEM_STEP_Y = 18;

    private static final int TINT_RGB = 0x0A1026;
    private static final int TINT_ALPHA = 26;

    private int cursor;
    private int count;

    public void enter() {
        count = 0;

        int[] opt = GameOptions.load();
        cursor = opt[GameOptions.IDX_LANGUAGE];
        if (cursor < 0 || cursor >= I18n.getLanguageCount()) {
            cursor = 0;
        }
    }

    public boolean update(int pressed) {
        if (count < 8) {
            count++;
        }

        if (UiInput.down(pressed)) {
            cursor = MenuCursor.move(cursor, 1, I18n.getLanguageCount());
        } else if (UiInput.up(pressed)) {
            cursor = MenuCursor.move(cursor, -1, I18n.getLanguageCount());
        } else if (UiInput.dismiss(pressed) || UiInput.secondary(pressed)) {
            commit();
            return true;
        }

        return false;
    }

    private void commit() {
        int[] opt = GameOptions.load();
        opt[GameOptions.IDX_LANGUAGE] = cursor;
        GameOptions.save(opt);

        I18nBootstrap.initFromOptions(opt);
    }

    public void render(Graphics g, ImageBank imgs, SpriteRenderer sprites) {
        if (g == null || imgs == null) {
            return;
        }

        UiDraw.drawRegion(g, imgs.get(SpriteSheetCatalog.TITLE_BACKGROUND_IMAGE), 0, 0, 0, 0, 240, 240);

        int wob = 4 - (count >> 1);
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

        g.drawImage(imgs.get(SpriteSheetCatalog.TITLE_PORTRAIT_IMAGE), 5, 93, Graphics.TOP | Graphics.LEFT);

        UiDraw.fillRectAlpha(g, 0, 0, 240, 240, TINT_RGB, TINT_ALPHA);

        int menuAreaH = TITLE_MENU_STEP * TITLE_MENU_ROWS;
        int listH = ITEM_H + ITEM_STEP_Y * (I18n.getLanguageCount() - 1);
        int y0 = TITLE_MENU_Y0 + ((menuAreaH - listH) >> 1);

        for (int i = 0; i < I18n.getLanguageCount(); i++) {
            int x;
            if ((i & 1) == 0) {
                x = 143 - i * 3 + wob + 8;
            } else {
                x = 143 - i * 3 - wob + 8;
            }
            int y = y0 + i * ITEM_STEP_Y;

            int spriteId;
            if (i == I18n.LANG_ZHS) {
                spriteId = SpriteSheetCatalog.SIMPLIFIED_LANGUAGE_ITEM_BASE + (i == cursor ? 1 : 0);
            } else {
                spriteId = SpriteSheetCatalog.LANGUAGE_ITEM_BASE + (i == cursor ? 3 : 0) + i;
            }
            sprites.draw(g, spriteId, x, y);
        }
    }

}
