package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
import touhou.battle.BattleSessionConfig;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplayHeader;
import touhou.replay.ReplaySlotStore;

public final class ReplayScreen {
    public static final class Result {
        public static final int KIND_BACK_TO_TITLE = 1;
        public static final int KIND_START_REPLAY = 2;
        public static final int KIND_OPEN_BT_RECEIVE = 4;
        public static final int KIND_OPEN_BT_SEND = 5;
        public static final int KIND_OPEN_EXPORT = 6;
        public static final int KIND_OPEN_IMPORT = 7;

        public final int kind;
        public final int slot;
        public final boolean bossOnly;

        public Result(int kind) {
            this.kind = kind;
            this.slot = -1;
            this.bossOnly = false;
        }

        public Result(int kind, int slot, boolean bossOnly) {
            this.kind = kind;
            this.slot = slot;
            this.bossOnly = bossOnly;
        }
    }

    // Cache fonts to avoid per-frame allocations.
    private static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font FONT_M = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    private static final int SCENE_LIST = 0;
    private static final int SCENE_ACTION = 1;
    private static final int SCENE_DELETE_CONFIRM = 2;
    private static final int SCENE_MESSAGE = 3;
    private static final int TRANSITION_FRAMES = 6;
    private static final int ACTION_COUNT = 7;
    private static final int ACTION_LINE_H = 14;

    public void enter() {
        cursor = 0;
        decoTick = 0;
        returnToList();
    }

    public void returnToList() {
        scene = SCENE_LIST;
        actionCursor = 0;
        msg = null;
        reloadSlots();
        cursor = slots.clampCursor(cursor);
        titleFrame = 0;
        contentFrame = 0;
        leaving = false;
        pendingResult = null;
    }

    public Result update(int pressed) {
        decoTick++;
        if (leaving) {
            if (pendingResult != null && titleFrame > 0) {
                titleFrame--;
            }
            if (contentFrame > 0) {
                contentFrame--;
            }
            if (contentFrame == 0 && (pendingResult == null || titleFrame == 0)) {
                leaving = false;
                if (pendingResult != null) {
                    Result result = pendingResult;
                    pendingResult = null;
                    return result;
                }
                scene = nextScene;
                if (scene != SCENE_MESSAGE) {
                    msg = null;
                }
            }
            return null;
        }
        if (titleFrame < TRANSITION_FRAMES || contentFrame < TRANSITION_FRAMES) {
            if (titleFrame < TRANSITION_FRAMES) {
                titleFrame++;
            }
            if (contentFrame < TRANSITION_FRAMES) {
                contentFrame++;
            }
            return null;
        }
        if (UiInput.cancel(pressed)) {
            if (scene == SCENE_LIST) {
                leave(new Result(Result.KIND_BACK_TO_TITLE));
                return null;
            }
            changeScene(SCENE_LIST);
            return null;
        }

        if (scene == SCENE_MESSAGE) {
            if (UiInput.dismiss(pressed)) {
                updateActionCache();
                changeScene(SCENE_ACTION);
            }
            return null;
        }

        if (scene == SCENE_DELETE_CONFIRM) {
            if (UiInput.confirm(pressed)) {
                boolean deleted = ReplaySlotStore.clearSlot(cursor);
                actionCursor = ACTION_BACK;
                reloadSlots();
                showMessage(UiText.get(deleted ? TextId.REPLAY_MSG_DELETED : TextId.REPLAY_SAVE_FAIL));
            }
            return null;
        }

        if (scene == SCENE_ACTION) {
            boolean moved = false;
            int moveDir = 1;
            if (UiInput.down(pressed)) {
                actionCursor = MenuCursor.move(actionCursor, 1, ACTION_COUNT);
                moved = true;
            } else if (UiInput.up(pressed)) {
                actionCursor = MenuCursor.move(actionCursor, -1, ACTION_COUNT);
                moved = true;
                moveDir = -1;
            } else if (UiInput.confirm(pressed)) {
                Result r = handleAction();
                if (r != null) {
                    leave(r);
                }
            }

            if (moved) {
                // Skip disabled actions (e.g., Play (Boss) when unavailable).
                ensureActionCursorEnabled(moveDir);
            }
            return null;
        }

        if (UiInput.down(pressed)) {
            cursor = MenuCursor.move(cursor, 1, slots.size());
        } else if (UiInput.up(pressed)) {
            cursor = MenuCursor.move(cursor, -1, slots.size());
        } else if (UiInput.confirm(pressed)) {
            actionCursor = 0;
            updateActionCache();
            // Place cursor on the first enabled action.
            ensureActionCursorEnabled(1);
            changeScene(SCENE_ACTION);
        }
        return null;
    }

    public void render(Graphics g, ImageBank imgs, SpriteRenderer sprites) {
        if (g == null) {
            return;
        }

        if (imgs != null) {
            this.imgs = imgs;
        }
        if (sprites != null) {
            this.sprites = sprites;
        }

        ReplayUi.renderBackground(g, this.imgs);

        int titleOffset = slideOffset(titleFrame, 44);
        g.translate(0, -titleOffset);
        ReplayUi.renderTitle(g, FONT, UiText.get(TextId.REPLAY_TITLE));
        g.translate(0, titleOffset);

        int contentOffset = slideOffset(contentFrame, 240);
        if (scene != SCENE_LIST) {
            contentOffset = -contentOffset;
        }
        g.translate(contentOffset, 0);
        if (scene == SCENE_LIST) {
            renderList(g, FONT);
        } else if (scene == SCENE_ACTION) {
            renderAction(g, FONT);
        } else if (scene == SCENE_DELETE_CONFIRM) {
            renderDeleteConfirm(g, FONT);
        } else {
            renderMessage(g, FONT);
        }
        g.translate(-contentOffset, 0);
    }

    private static final int ACTION_PLAY = 0;
    private static final int ACTION_PLAY_BOSS = 1;
    private static final int ACTION_NET = 2;
    private static final int ACTION_BT = 3;
    private static final int ACTION_FILE = 4;
    private static final int ACTION_DELETE = 5;
    private static final int ACTION_BACK = 6;

    private int scene;
    private int cursor;
    private int actionCursor;
    private int titleFrame;
    private int contentFrame;
    private int nextScene;
    private boolean leaving;
    private Result pendingResult;

    // Marquee tick for replay names.
    private int decoTick;

    private ImageBank imgs;
    private SpriteRenderer sprites;

    // Cache derived strings for the selected slot to avoid per-frame allocations.
    private int cachedSlot;
    private boolean cachedHasData;
    private String cachedTitle;
    private String cachedUnit;
    private String cachedDifficulty;
    private String cachedScore;
    private boolean cachedSpellPractice;
    private boolean cachedHasBoss;
    private boolean cachedImported;

    private final ReplaySlotListModel slots = new ReplaySlotListModel();

    private String msg;

    private final int[] tmpSpriteMetrics = new int[4];

    private void changeScene(int target) {
        nextScene = target;
        leaving = true;
    }

    private void leave(Result result) {
        pendingResult = result;
        leaving = true;
    }

    private static int slideOffset(int frame, int distance) {
        int remaining = TRANSITION_FRAMES - frame;
        return distance * remaining * remaining / (TRANSITION_FRAMES * TRANSITION_FRAMES);
    }

    private void reloadSlots() {
        slots.reload();
        cachedSlot = -1;
    }

    private void updateActionCache() {
        if (cachedSlot == cursor) {
            return;
        }
        cachedSlot = cursor;

        ReplayHeader h = slots.header(cursor);
        cachedHasData = slots.hasData(cursor);

        cachedSpellPractice = (h != null && h.getMode() == BattleSessionConfig.MODE_SPELL_PRACTICE);

        if (!cachedHasData || h == null) {
            cachedTitle = UiText.get(TextId.REPLAY_LABEL_EMPTY);
            cachedUnit = "";
            cachedDifficulty = "";
            cachedScore = "";
            cachedHasBoss = false;
            cachedImported = false;
            return;
        }

        cachedTitle = ReplayUi.decodeSlotName(slots.nameBytes(cursor));
        if (cachedTitle.length() == 0) {
            cachedTitle = cachedSpellPractice ? ReplayUi.formatDetail(h) : ReplayUi.stageName(h.getStage());
        }
        cachedUnit = ReplayUi.formatUnit(h);
        cachedDifficulty = cachedSpellPractice ? UiText.get(TextId.REPLAY_TAG_SP) : ReplayUi.difficultyName(h.getDifficulty());
        cachedScore = String.valueOf(h.getScore());
        cachedHasBoss = !cachedSpellPractice && slots.hasBossOnly(cursor);
        cachedImported = (h.getFlag() == ReplayHeader.FLAG_IMPORTED);
    }

    private Result handleAction() {
        int act = actionCursor;

        if (act == ACTION_BACK) {
            changeScene(SCENE_LIST);
            return null;
        }

        if (act == ACTION_DELETE) {
            if (!isActionEnabled(ACTION_DELETE)) {
                return null;
            }
            changeScene(SCENE_DELETE_CONFIRM);
            return null;
        }

        if (act == ACTION_NET) {
            if (!isActionEnabled(act)) {
                return null;
            }
            showMessage(UiText.get(TextId.REPLAY_MSG_NOT_IMPLEMENTED));
            return null;
        }

        if (act == ACTION_BT) {
            if (!isActionEnabled(act)) {
                return null;
            }
            return new Result(cachedHasData ? Result.KIND_OPEN_BT_SEND : Result.KIND_OPEN_BT_RECEIVE, cursor, false);
        }
        if (act == ACTION_FILE) {
            if (!isActionEnabled(act)) {
                return null;
            }
            return new Result(cachedHasData ? Result.KIND_OPEN_EXPORT : Result.KIND_OPEN_IMPORT, cursor, false);
        }

        boolean hasData = slots.hasData(cursor);
        if (!hasData) {
            showMessage(UiText.get(TextId.REPLAY_MSG_EMPTY_SLOT));
            return null;
        }

        if (act == ACTION_PLAY_BOSS) {
            if (!isActionEnabled(ACTION_PLAY_BOSS)) {
                return null;
            }
            return new Result(Result.KIND_START_REPLAY, cursor, true);
        }
        if (act == ACTION_PLAY) {
            return new Result(Result.KIND_START_REPLAY, cursor, false);
        }

        showMessage(UiText.get(TextId.REPLAY_MSG_UNKNOWN_ACTION));
        return null;
    }

    void showMessage(String s) {
        msg = s;
        changeScene(SCENE_MESSAGE);
    }

    private void renderList(Graphics g, Font font) {
        ReplayUi.renderSlotListCompact(
                g, font, cursor, slots, UiText.get(TextId.COMMON_SLOT_PREFIX), decoTick, false);
    }

    private void renderAction(Graphics g, Font font) {
        updateActionCache();
        UiDraw.drawString2(g, font, UiText.get(TextId.COMMON_SLOT_PREFIX) + (cursor + 1),
                228, 52, 2, 0xB0B0B0, 0x404040);
        UiDraw.drawStringMarquee2(g, FONT_M, cachedTitle, 14, 65, 212, 1,
                0xFFFFFF, 0x303040, decoTick);
        ReplayUi.renderSelection(g, 129 + actionCursor * ACTION_LINE_H, 18);
        if (cachedHasData) {
            ReplayHeader h = slots.header(cursor);
            if (cachedImported) {
                UiDraw.drawString2(g, font, UiText.get(TextId.REPLAY_TAG_IMPORTED), 18, 52, 0, 0xB0B0B0, 0x404040);
            }
            UiDraw.drawString2(g, font, cachedDifficulty, 18, 89, 0, 0xB0B0B0, 0x404040);
            UiDraw.drawString2(g, font, cachedUnit, 226, 89, 2, 0xFFFFFF, 0x303040);
            g.setColor(0x7F394D);
            g.drawLine(12, 99, 228, 99);
            UiDraw.drawString2(g, font, UiText.get(TextId.REPLAY_LABEL_SCORE), 18, 118, 0, 0xB0B0B0, 0x404040);
            UiDraw.drawString2(g, font, cachedScore, 226, 118, 2, 0xFFFFFF, 0x303040);

            int sprite = SpriteSheetCatalog.CHARACTER_PORTRAIT_BASE + h.getChara();
            sprites.getSizeAndAnchor(sprite, tmpSpriteMetrics);
            sprites.draw(g, sprite, 43 - (tmpSpriteMetrics[0] >> 1) + tmpSpriteMetrics[2],
                    231 - tmpSpriteMetrics[1] + tmpSpriteMetrics[3]);
        }

        for (int i = 0; i < ACTION_COUNT; i++) {
            boolean enabled = isActionEnabled(i);
            boolean selected = (i == actionCursor);
            int main = enabled ? (selected ? 0xFF4F4F : 0xFFFFFF) : 0x666676;
            int shadow = selected ? 0x771F1F : 0x303040;
            int y = 142 + i * ACTION_LINE_H;
            if (selected) {
                UiDraw.drawString2(g, font, ">", 92, y, 0, main, shadow);
            }
            UiDraw.drawStringMarquee2(g, font, actionLabel(i), 101, y, 129, 0, main, shadow, decoTick);
        }
    }

    private boolean isActionEnabled(int act) {
        if (act == ACTION_BACK) {
            return true;
        }
        if (act == ACTION_NET || act == ACTION_BT || act == ACTION_FILE) {
            return !cachedHasData || !cachedImported;
        }
        if (!cachedHasData) {
            return false;
        }
        if (act == ACTION_PLAY_BOSS) {
            if (cachedSpellPractice) {
                return false;
            }
            return cachedHasBoss;
        }
        return true;
    }

    private void ensureActionCursorEnabled(int dir) {
        while (!isActionEnabled(actionCursor)) {
            actionCursor = MenuCursor.move(actionCursor, dir, ACTION_COUNT);
        }
    }

    private String actionLabel(int act) {
        if (act == ACTION_PLAY) {
            return UiText.get(TextId.REPLAY_ACTION_PLAY);
        }
        if (act == ACTION_PLAY_BOSS) {
            return UiText.get(TextId.REPLAY_ACTION_PLAY_BOSS);
        }
        if (act == ACTION_DELETE) {
            return UiText.get(TextId.REPLAY_ACTION_DELETE);
        }
        if (act == ACTION_NET) {
            return UiText.get(cachedHasData ? TextId.REPLAY_ACTION_NET_SHARE : TextId.REPLAY_ACTION_NET_RECEIVE);
        }
        if (act == ACTION_BT) {
            return UiText.get(cachedHasData ? TextId.REPLAY_ACTION_BT_SHARE : TextId.REPLAY_ACTION_BT_RECEIVE);
        }
        if (act == ACTION_FILE) {
            return UiText.get(cachedHasData ? TextId.REPLAY_ACTION_EXPORT : TextId.REPLAY_ACTION_IMPORT);
        }
        return UiText.get(TextId.REPLAY_ACTION_BACK);
    }

    private void renderDeleteConfirm(Graphics g, Font font) {
        UiDraw.fillRectAlpha(g, 0, 88, 240, 68, 0x000000, 208);
        UiDraw.drawString2(g, font,
                UiText.get(TextId.REPLAY_DELETE_CONFIRM_PREFIX) + (cursor + 1) + UiText.get(TextId.REPLAY_DELETE_CONFIRM_SUFFIX),
                120, 115, 1, 0xFF4F4F, 0x771F1F);
        UiDraw.drawStringMarquee2(g, font, cachedTitle, 14, 137, 212, 1, 0xFFFFFF, 0x303040, decoTick);
    }

    private void renderMessage(Graphics g, Font font) {
        ReplayUi.renderMessageBox(g, font, FONT_M, msg, 94, decoTick);
    }
}
