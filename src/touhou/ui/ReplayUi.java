package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.SpriteRenderer;
import touhou.ImageBank;
import touhou.i18n.SpellText;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplayHeader;

final class ReplayUi {
    // Shared replay presentation for save, playback, send and receive screens.

    private ReplayUi() {
    }

    static void renderBackground(Graphics g, SpriteRenderer sprites) {
        renderBackground(g, sprites == null ? null : sprites.getImages());
    }

    static void renderBackground(Graphics g, ImageBank images) {
        Image background = images == null ? null : images.get(37);
        if (background != null) {
            UiDraw.drawRegion(g, background, 0, 0, 0, 0, 240, 240);
            UiDraw.fillRectAlpha(g, 0, 0, 240, 240, 0x000000, 96);
        } else {
            g.setColor(0x000000);
            g.fillRect(0, 0, 240, 240);
        }
        g.setColor(0x005500);
        g.drawRect(3, 3, 234, 234);
        g.setColor(0x00AA00);
        g.drawRect(6, 6, 228, 228);
        g.setColor(0x00FF00);
        g.drawRect(9, 9, 222, 222);
    }

    static void renderPreviewCard(Graphics g, Font font, Font mediumFont,
            ReplayPreview preview, String emptyText, int animationTick) {
        int cardX = 16;
        int cardY = 32;
        int cardW = 208;
        int cardH = 78;
        UiDraw.fillRectAlpha(g, cardX, cardY, cardW, cardH, 0x000000, 160);
        g.setColor(0x00FF00);
        g.drawRect(cardX, cardY, cardW - 1, cardH - 1);

        if (preview == null || !preview.hasData()) {
            UiDraw.drawString2(g, mediumFont, emptyText, 120, cardY + 26, 1, 0xBBBBBB, 0x333333);
            return;
        }

        boolean spellPractice = preview.isSpellPractice();
        int titleWidth = spellPractice ? cardW - 42 : cardW - 24;
        UiDraw.drawStringMarquee2(g, mediumFont, preview.title(), cardX + 12, cardY + 22,
                titleWidth, 1, 0xFFFFFF, 0x222222, animationTick);
        if (spellPractice) {
            UiDraw.drawString2(g, font, UiText.get(TextId.REPLAY_TAG_SP),
                    cardX + cardW - 12, cardY + 22, 2, 0xA0FFA0, 0x224422);
        }
        UiDraw.drawString2(g, font, preview.unit(), cardX + 12, cardY + 42, 0, 0xDDDDDD, 0x333333);
        g.setColor(0x005500);
        g.drawLine(cardX + 8, cardY + 54, cardX + cardW - 8, cardY + 54);
        UiDraw.drawString2(g, font, UiText.get(TextId.REPLAY_LABEL_SCORE),
                cardX + 12, cardY + 66, 0, 0xAAAAAA, 0x333333);
        UiDraw.drawString2(g, font, preview.score(),
                cardX + cardW - 12, cardY + 66, 2, 0xFFFFFF, 0x222222);
    }

    static void renderSlotListCompact(Graphics g, Font font, int cursor,
            ReplaySlotListModel slots, String slotPrefix, int animationTick) {
        renderSlotListCompact(g, font, cursor, slots, slotPrefix, animationTick, true);
    }

    static void renderSlotListCompact(Graphics g, Font font, int cursor,
            ReplaySlotListModel slots, String slotPrefix, int animationTick, boolean drawTitle) {
        if (drawTitle) {
            UiDraw.drawString2(g, font, UiText.get(TextId.REPLAY_SLOTLIST_TITLE), 120, 25, 1, 0xFFFFFF, 0x777777);
        }
        renderSlotList(g, font, cursor, slots, slotPrefix, animationTick, 49, 4, 80);
    }

    static void renderSlotListBelowPreview(Graphics g, Font font, int cursor,
            ReplaySlotListModel slots, String slotPrefix, int animationTick) {
        renderSlotList(g, font, cursor, slots, slotPrefix, animationTick, 118, 3, 72);
    }

    private static void renderSlotList(Graphics g, Font font, int cursor,
            ReplaySlotListModel slots, String slotPrefix, int animationTick,
            int firstBoxY, int maximumVisible, int unitX) {
        int slotCount = slots.size();
        int visible = slotCount < maximumVisible ? slotCount : maximumVisible;
        int start = MenuCursor.visibleStart(cursor, slotCount, visible);

        int lineH = font.getHeight();
        int baselinePos = font.getBaselinePosition();
        int lineGap = 2;

        int boxStepY = 40;
        for (int row = 0; row < visible; row++) {
            int i = start + row;
            boolean sel = (i == cursor);
            int main = sel ? 0xFFFFFF : 0x777777;
            int sh = sel ? 0x444444 : 0x222222;

            int boxX = 12;
            int boxY = firstBoxY + row * boxStepY;
            int boxW = 216;
            int boxH = 34;
            if (sel) {
                g.setColor(0x001A00);
                g.fillRect(boxX, boxY, boxW, boxH);
                g.setColor(0x00AA00);
                g.drawRect(boxX, boxY, boxW, boxH);
            }

            int totalTextH = lineH + lineGap + lineH;
            int padY = (boxH - totalTextH) >> 1;
            if (padY < 0) {
                padY = 0;
            }

            int line1BaseY = boxY + padY + baselinePos;
            int line2BaseY = line1BaseY + lineH + lineGap;

            UiDraw.drawString2(g, font, slotPrefix + (i + 1), 18, line1BaseY, 0, main, sh);

            ReplayHeader h = slots.header(i);
            if (h != null && h.getFlag() != 0) {
                // Line 1: unit + score (no tags, avoids overlap).
                UiDraw.drawString2(g, font, formatUnit(h), unitX, line1BaseY, 0, main, sh);
                UiDraw.drawString2(g, font, String.valueOf(h.getScore()), boxX + boxW - 6, line1BaseY, 2, main, sh);

                // Line 2 right: compact tags (Boss/Imported/SP only).
                int tagMain = sel ? 0xA0FFA0 : 0x558855;
                int tagSh = sel ? 0x224422 : 0x112211;
                String listTags = formatListTags(h, slots.hasBossOnly(i));
                int tagsW = 0;
                if (listTags.length() > 0) {
                    UiDraw.drawString2(g, font, listTags, boxX + boxW - 6, line2BaseY, 2, tagMain, tagSh);
                    tagsW = UiDraw.stringWidth(font, listTags) + 6;
                }

                // Line 2 left: detail or spell card name.
                int detailClipW = boxW - 12 - tagsW;
                if (detailClipW < 20) { detailClipW = 20; }

                if (h.getMode() == 3) {
                    int spellId = h.getSpellId();
                    String name = getSpellName(spellId);

                    int clipX = boxX + 6;
                    int nameX = clipX;
                    int nameW = detailClipW;
                    if (nameW > 0) {
                        int fastTick = animationTick + (animationTick >> 1);
                        UiDraw.drawStringMarquee2(g, font, name, nameX, line2BaseY, nameW, 0, main, sh, fastTick);
                    }
                } else {
                    String detail = formatDetail(h);
                    UiDraw.drawStringMarquee2(g, font, detail, boxX + 6, line2BaseY, detailClipW, 0, main, sh, animationTick);
                }
            } else {
                UiDraw.drawString2(g, font, "-", boxX + boxW - 6, line1BaseY, 2, main, sh);
            }
        }

        if (slotCount > visible) {
            int trackX = 232;
            int trackY = firstBoxY;
            int trackH = (visible - 1) * boxStepY + 34;

            g.setColor(0x00AA00);
            g.drawLine(trackX, trackY, trackX, trackY + trackH - 1);

            int denom = slotCount - visible;
            int knobH = (trackH * visible) / slotCount;
            if (knobH < 10) {
                knobH = 10;
            }
            if (knobH > trackH) {
                knobH = trackH;
            }
            int knobY = trackY;
            if (denom > 0) {
                knobY = trackY + ((trackH - knobH) * start) / denom;
            }

            g.setColor(0x001A00);
            g.fillRect(trackX - 2, knobY, 5, knobH);
            g.setColor(0x00FF00);
            g.drawRect(trackX - 2, knobY, 4, knobH - 1);
        }
    }

    static void renderSaveResultBox(Graphics g, Font font, boolean ok, int boxY) {
        g.setColor(0x000000);
        g.fillRect(24, boxY, 192, 36);
        g.setColor(0x00FF00);
        g.drawRect(25, boxY + 1, 190, 34);

        String msg = ok ? UiText.get(TextId.REPLAY_SAVE_OK) : UiText.get(TextId.REPLAY_SAVE_FAIL);
        UiDraw.drawString2(g, font, msg, 120, boxY + 20, 1, 0xFFFFFF, 0x444444);
    }

    static void renderMessageBox(Graphics g, Font font, Font mediumFont,
            String message, int boxY, int animationTick) {
        int boxX = 16;
        int boxW = 208;
        int boxH = 60;
        UiDraw.fillRectAlpha(g, boxX, boxY, boxW, boxH, 0x000000, 180);
        g.setColor(0x00FF00);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
        UiDraw.drawStringMarquee2(g, mediumFont, message == null ? "" : message,
                boxX + 8, boxY + 22, boxW - 16, 1, 0xFFFFFF, 0x444444, animationTick);
        UiDraw.drawString2(g, font, "[OK] " + UiText.get(TextId.BT_HINT_OK),
                120, boxY + 46, 1, 0x88FF88, 0x224422);
    }

    static String difficultyName(int v) {
        switch (v) {
            case 0:
                return UiText.get(TextId.REPLAY_DIFFICULTY_EASY);
            case 1:
                return UiText.get(TextId.REPLAY_DIFFICULTY_NORMAL);
            case 2:
                return UiText.get(TextId.REPLAY_DIFFICULTY_HARD);
            case 3:
                return UiText.get(TextId.REPLAY_DIFFICULTY_LUNATIC);
            case 4:
                return UiText.get(TextId.REPLAY_DIFFICULTY_EXTRA);
            default:
                return "-";
        }
    }

    static String stageName(int v) {
        switch (v) {
            case 0:
                return UiText.get(TextId.REPLAY_STAGE_1);
            case 1:
                return UiText.get(TextId.REPLAY_STAGE_2);
            case 2:
                return UiText.get(TextId.REPLAY_STAGE_3);
            case 3:
                return UiText.get(TextId.REPLAY_STAGE_4);
            case 4:
                return UiText.get(TextId.REPLAY_STAGE_5);
            case 5:
                return UiText.get(TextId.REPLAY_STAGE_6);
            case 6:
                return UiText.get(TextId.REPLAY_STAGE_EX);
            default:
                return "-";
        }
    }

    static String formatUnit(ReplayHeader h) {
        if (h == null) {
            return "-";
        }
        int c = h.getChara();
        int t = h.getType();
        String n;
        if (c == 0) {
            n = UiText.get(TextId.REPLAY_CHARA_REIMU);
        } else if (c == 1) {
            n = UiText.get(TextId.REPLAY_CHARA_MARISA);
        } else if (c == 2) {
            n = UiText.get(TextId.REPLAY_CHARA_ALICE);
        } else {
            n = "?";
        }
        String suf = (t == 0) ? "A" : "B";
        return n + suf;
    }

    // Compact tags for list line 2 (omits redundant ST/SP mode indicator).
    static String formatListTags(ReplayHeader h, boolean hasBossOnly) {
        if (h == null) {
            return "";
        }
        String tag = "";
        if (h.getMode() == 3) {
            tag = UiText.get(TextId.REPLAY_TAG_SP);
        }
        if (h.getFlag() == ReplayHeader.FLAG_IMPORTED) {
            if (tag.length() > 0) { tag += " "; }
            tag += UiText.get(TextId.REPLAY_TAG_IMPORTED);
        }
        if (h.getMode() != 3 && hasBossOnly) {
            if (tag.length() > 0) { tag += " "; }
            tag += UiText.get(TextId.REPLAY_TAG_ST);
        }
        return tag;
    }

    static String formatDetail(ReplayHeader h) {
        if (h == null) {
            return "-";
        }

        if (h.getMode() == 3) {
            return getSpellName(h.getSpellId());
        }

        return difficultyName(h.getDifficulty()) + " " + stageName(h.getStage());
    }

    // Decode fixed 32-byte zero-terminated UTF-8 slot name.
    static String decodeSlotName(byte[] name32) {
        if (name32 == null) {
            return "";
        }

        int n = 0;
        while (n < name32.length) {
            if (name32[n] == 0) {
                break;
            }
            n++;
        }
        if (n <= 0) {
            return "";
        }

        try {
            return new String(name32, 0, n, "UTF-8").trim();
        } catch (Throwable t) {
            return "";
        }
    }

    // Lazy-load localized spell card names for spell practice replay display.
    private static String getSpellName(int id) {
        if (id == 255) {
            return "-";
        }
        String name = SpellText.getName(id);
        if (name != null) {
            return name;
        }
        return UiText.get(TextId.REPLAY_SPELLNO_PREFIX) + (id + 1);
    }
}
