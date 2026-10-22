package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ResultStats;
import touhou.GameProgress;
import touhou.i18n.SpellText;
import touhou.i18n.I18n;
import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class ResultScreen {
    public static final class Result {
        public static final int KIND_NONE = 0;
        public static final int KIND_BACK_TO_TITLE = 1;

        public final int kind;

        public Result(int kind) {
            this.kind = kind;
        }
    }

    private static final int SCENE_MENU = 0;
    private static final int SCENE_SPELLCARD = 1;
    private static final int SCENE_SCORE = 2;
    private static final int SCENE_OTHER = 4;

    private static final int PAGE_SIZE = 10;

    private static String menuText(int idx) {
        switch (idx) {
            case 0:
                return UiText.get(TextId.RESULT_MENU_0);
            case 1:
                return UiText.get(TextId.RESULT_MENU_1);
            case 2:
                return UiText.get(TextId.RESULT_MENU_2);
            case 3:
                return UiText.get(TextId.RESULT_MENU_3);
            case 4:
                return UiText.get(TextId.RESULT_MENU_4);
            default:
                return "";
        }
    }

    private static String charaText(int idx) {
        switch (idx) {
            case 0:
                return UiText.get(TextId.RESULT_CHARA_0);
            case 1:
                return UiText.get(TextId.RESULT_CHARA_1);
            case 2:
                return UiText.get(TextId.RESULT_CHARA_2);
            case 3:
                return UiText.get(TextId.RESULT_CHARA_3);
            case 4:
                return UiText.get(TextId.RESULT_CHARA_4);
            case 5:
                return UiText.get(TextId.RESULT_CHARA_5);
            case 6:
                return UiText.get(TextId.RESULT_CHARA_6);
            default:
                return "";
        }
    }

    private int scene;
    private int cursorUD;
    private int cursorLR;
    private int cursor2;

    private int effect1;
    private int effect2;

    public void enter() {
        scene = SCENE_MENU;
        cursorUD = 0;
        cursorLR = 0;
        cursor2 = 0;
        effect1 = 255;
        effect2 = 0;
    }

    public Result update(int pressed) {
        if (scene == SCENE_MENU) {
            effect1 = (effect1 << 1) + 1;
            if (effect1 > 255) {
                effect1 = 255;
            }
            effect2 >>= 1;
        } else {
            effect1 >>= 1;
            effect2 = (effect2 << 1) + 1;
            if (effect2 > 255) {
                effect2 = 255;
            }
        }

        if (UiInput.cancel(pressed)) {
            if (scene == SCENE_MENU) {
                return new Result(Result.KIND_BACK_TO_TITLE);
            }
            scene = SCENE_MENU;
            cursorLR = 0;
            cursor2 = 0;
            return null;
        }

        if (scene == SCENE_MENU) {
            if (UiInput.up(pressed)) {
                cursorUD = MenuCursor.move(cursorUD, -1, 5);
            } else if (UiInput.down(pressed)) {
                cursorUD = MenuCursor.move(cursorUD, 1, 5);
            } else if (UiInput.confirm(pressed)) {
                if (cursorUD == 0) {
                    scene = SCENE_SPELLCARD;
                    cursorLR = 0;
                    cursor2 = 0;
                } else if (cursorUD == 1) {
                    scene = SCENE_SCORE;
                    cursorLR = 0;
                    cursor2 = 0;
                } else if (cursorUD == 2) {
                    scene = SCENE_SCORE;
                    cursorLR = 0;
                    cursor2 = 0;
                } else if (cursorUD == 3) {
                    scene = SCENE_OTHER;
                } else {
                    return new Result(Result.KIND_BACK_TO_TITLE);
                }
            }
            return null;
        }

        if (scene == SCENE_SPELLCARD) {
            int pageCount = (GameProgress.SPELL_COUNT - 1) / PAGE_SIZE + 1;
            if (UiInput.left(pressed)) {
                if (cursorLR > 0) {
                    cursorLR--;
                }
            } else if (UiInput.right(pressed)) {
                if (cursorLR < pageCount - 1) {
                    cursorLR++;
                }
            } else if (UiInput.up(pressed)) {
                cursor2 = MenuCursor.move(cursor2, -1, 7);
            } else if (UiInput.down(pressed)) {
                cursor2 = MenuCursor.move(cursor2, 1, 7);
            }
            return null;
        }

        if (scene == SCENE_SCORE) {
            if (UiInput.left(pressed)) {
                if (cursorLR != 0) {
                    cursorLR = 0;
                }
            } else if (UiInput.right(pressed)) {
                if (cursorLR != 1) {
                    cursorLR = 1;
                }
            } else if (UiInput.up(pressed)) {
                cursor2 = MenuCursor.move(cursor2, -1, 6);
            } else if (UiInput.down(pressed)) {
                cursor2 = MenuCursor.move(cursor2, 1, 6);
            }
            return null;
        }

        if (scene == SCENE_OTHER) {
            return null;
        }

        return null;
    }

    public void render(Graphics g) {
        if (g == null) {
            return;
        }

        UiDraw.drawGradation(g, 0, 0, 240, 240, 0x000000, 9540095, 32);

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_TITLE), 120 - effect2, 22, 1, 16733525);

        for (int i = 0; i < 5; i++) {
            int main;
            if (i == cursorUD && scene == SCENE_MENU) {
                main = 0xFFFFFF;
            } else {
                main = 6710886;
            }
            UiDraw.drawStringPlain(g, font, menuText(i), 30 - effect2, 50 + i * 20, 0, main);
        }

        if (scene == SCENE_MENU) {
            renderPreview(g, font);
            return;
        }

        if (scene == SCENE_SPELLCARD) {
            renderSpellCard(g, font);
            return;
        }

        if (scene == SCENE_SCORE) {
            renderScore(g, font);
            return;
        }

        if (scene == SCENE_OTHER) {
            renderOther(g, font);
        }
    }

    private void renderPreview(Graphics g, Font font) {
        if (cursorUD == 0) {
            renderSpellCard(g, font);
        } else if (cursorUD == 1) {
            renderScore(g, font);
        } else if (cursorUD == 3) {
            renderOther(g, font);
        } else if (cursorUD == 2) {
            UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_PREVIEW_NET_NOT_IMPLEMENTED), 120, 120, 1, 0xFFFFFF);
        }
    }

    private void renderSpellCard(Graphics g, Font font) {
        int spellTotal = GameProgress.SPELL_COUNT;
        int pageCount = (spellTotal - 1) / PAGE_SIZE + 1;

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_SPELLCARD_TITLE_PREFIX) + charaText(cursor2) + UiText.get(TextId.RESULT_SPELLCARD_TITLE_SUFFIX), 120 + effect1, 22, 1, 16711680);
        UiDraw.drawStringPlain(g, font, String.valueOf(cursorLR + 1) + "/" + pageCount, 220 + effect1, 40, 2, 16720418);

        int[] bonus = new int[GameProgress.SPELL_COUNT];
        int[] seen = new int[GameProgress.SPELL_COUNT];
        GameProgress.INSTANCE.copySpellCounts(cursor2 - 1, bonus, seen);

        int captured = 0;
        for (int i = 0; i < GameProgress.SPELL_COUNT; i++) {
            if (bonus[i] != 0) {
                captured++;
            }
        }

        UiDraw.drawStringOutline(g, font, UiText.get(TextId.RESULT_SPELLCARD_CAPTURED_PREFIX) + captured + "/" + spellTotal, 20 + effect1, 40, 0, 2237183, 0xFFFFFF);

        int start = cursorLR * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < spellTotal; i++) {
            int id = start + i;
            String name = getSpellDisplayName(id, seen[id] != 0);

            // Only ellipsize spell names for languages that can overflow horizontally.
            boolean ellipsize = (I18n.getLanguageId() == I18n.LANG_EN);
            if (ellipsize) {
                int colW = 235 - 20;
                int maxW = (colW * 4) / 5;
                if (maxW > 0) {
                    name = UiDraw.ellipsize(font, name, maxW);
                }
            }
            UiDraw.drawStringPlain(g, font, name, 20 + effect1, 60 + i * 18, 0, 0xFFFFFF);
            UiDraw.drawStringPlain(g, font, String.valueOf(bonus[id]) + "/" + String.valueOf(seen[id]), 235 + effect1, 60 + i * 18, 2, 0xFFFFFF);
            if (bonus[id] != 0) {
                UiDraw.drawStringPlain(g, font, UiText.get(TextId.SPELL_PRACTICE_STAR), 5 + effect1, 60 + i * 18, 0, 0xFFFFFF);
            }
            g.setColor(0x777777);
            g.drawLine(20 + effect1, 60 + i * 18 + 4, 235 + effect1, 60 + i * 18 + 4);
        }
    }

    private void renderScore(Graphics g, Font font) {
        int unit = cursor2;
        if (unit < 0) {
            unit = 0;
        }
        if (unit > 5) {
            unit = 5;
        }

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_SCORE_TITLE_PREFIX) + charaText(unit + 1) + UiText.get(TextId.RESULT_SCORE_TITLE_SUFFIX), 120 + effect1, 22, 1, 16733525);

        int[] scores = GameProgress.INSTANCE.getScoreList(unit);
        int baseY = 40;

        if (cursorLR == 0) {
            drawScoreBlock(g, font, UiText.get(TextId.RESULT_SCORE_EASY), 30 - effect1, baseY + 0, scores, 0, 7829503);
            drawScoreBlock(g, font, UiText.get(TextId.RESULT_SCORE_NORMAL), 70 + effect1, baseY + 48, scores, 3, 7864183);
            drawScoreBlock(g, font, UiText.get(TextId.RESULT_SCORE_HARD), 110 - effect1, baseY + 96, scores, 6, 16742263);
            drawScoreBlock(g, font, UiText.get(TextId.RESULT_SCORE_LUNATIC), 150 + effect1, baseY + 144, scores, 9, 16777079);
        } else {
            UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_SCORE_EXTRA), 75 - effect1, baseY + 84, 0, 10448895);
            UiDraw.drawStringPlain(g, font, "1: " + String.valueOf(scores[12]), 75 - effect1, baseY + 96, 0, 11501567);
            UiDraw.drawStringPlain(g, font, "2: " + String.valueOf(scores[13]), 75 - effect1, baseY + 108, 0, 11501567);
            UiDraw.drawStringPlain(g, font, "3: " + String.valueOf(scores[14]), 75 - effect1, baseY + 120, 0, 11501567);
            UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_SCORE_UPLOAD_NOT_IMPLEMENTED), 120, 230, 1, 0xFFFFFF);
        }
    }

    private void renderOther(Graphics g, Font font) {
        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_TITLE), 120 + effect1, 22, 1, 16711680);

        int[] ud = GameProgress.INSTANCE.getUserData();

        g.setColor(0xFFFFFF);
        g.drawRect(30 + effect1, 35, 180, 80);
        g.drawRect(30 + effect1, 125, 180, 96);
        g.drawLine(120 + effect1, 35, 120 + effect1, 115);
        g.drawLine(90 + effect1, 125, 90 + effect1, 221);
        g.drawLine(150 + effect1, 125, 150 + effect1, 221);

        for (int i = 0; i < 4; i++) {
            g.drawLine(30 + effect1, 51 + i * 16, 210 + effect1, 51 + i * 16);
        }
        for (int i = 0; i < 5; i++) {
            g.drawLine(30 + effect1, 141 + i * 16, 210 + effect1, 141 + i * 16);
        }

        int main = 0xFFFFFF;

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_BOOT_TIME), 115 + effect1, 48, 2, main);
        UiDraw.drawStringPlain(g, font, formatTimeSeconds(ResultStats.INSTANCE.getBootTimeSecondsNow()), 205 + effect1, 48, 2, main);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_PLAY_TIME), 115 + effect1, 64, 2, main);
        UiDraw.drawStringPlain(g, font, formatTimeSeconds(ud[1]), 205 + effect1, 64, 2, main);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_TOTAL_CLEAR), 115 + effect1, 80, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(ud[2]), 205 + effect1, 80, 2, main);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_CONTINUE_COUNT), 115 + effect1, 96, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(ud[3]), 205 + effect1, 96, 2, main);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_RETRY_COUNT), 115 + effect1, 112, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(ud[4]), 205 + effect1, 112, 2, main);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_PLAY_COUNT), 60 + effect1, 138, 1, main);
        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_NORMAL), 120 + effect1, 138, 1, main);
        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_PRACTICE), 180 + effect1, 138, 1, main);

        drawPlayCountRow(g, font, UiText.get(TextId.REPLAY_DIFFICULTY_EASY), 154, ud[9], ud[5], main);
        drawPlayCountRow(g, font, UiText.get(TextId.REPLAY_DIFFICULTY_NORMAL), 170, ud[10], ud[6], main);
        drawPlayCountRow(g, font, UiText.get(TextId.REPLAY_DIFFICULTY_HARD), 186, ud[11], ud[7], main);
        drawPlayCountRow(g, font, UiText.get(TextId.REPLAY_DIFFICULTY_LUNATIC), 202, ud[12], ud[8], main);
        UiDraw.drawStringPlain(g, font, UiText.get(TextId.RESULT_OTHER_EXTRA), 85 + effect1, 218, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(ud[13]), 145 + effect1, 218, 2, main);
        UiDraw.drawStringPlain(g, font, "-", 205 + effect1, 218, 2, main);
    }

    private static String getSpellDisplayName(int id, boolean seen) {
        if (!seen) {
            return "\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f\uff1f";
        }
        String name = SpellText.getName(id);
        if (name != null) {
            return name;
        }
        return "SpellNo." + (id + 1);
    }

    private static String formatTimeSeconds(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int h = seconds / 3600;
        int m = (seconds / 60) % 60;
        int s = seconds % 60;

        String hh = (h < 10) ? ("0" + h) : String.valueOf(h);
        String mm = (m < 10) ? ("0" + m) : String.valueOf(m);
        String ss = (s < 10) ? ("0" + s) : String.valueOf(s);
        return hh + ":" + mm + ":" + ss;
    }

    private void drawPlayCountRow(Graphics g, Font font, String label, int y, int normal, int practice, int main) {
        UiDraw.drawStringPlain(g, font, label, 85 + effect1, y, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(normal), 145 + effect1, y, 2, main);
        UiDraw.drawStringPlain(g, font, String.valueOf(practice), 205 + effect1, y, 2, main);
    }

    private void drawScoreBlock(Graphics g, Font font, String label, int x, int y, int[] scores, int baseIndex, int cMain) {
        UiDraw.drawStringPlain(g, font, label, x, y + 0, 0, cMain);
        UiDraw.drawStringPlain(g, font, "1: " + String.valueOf(scores[baseIndex + 0]), x, y + 12, 0, cMain);
        UiDraw.drawStringPlain(g, font, "2: " + String.valueOf(scores[baseIndex + 1]), x, y + 24, 0, cMain);
        UiDraw.drawStringPlain(g, font, "3: " + String.valueOf(scores[baseIndex + 2]), x, y + 36, 0, cMain);
    }

    
}
