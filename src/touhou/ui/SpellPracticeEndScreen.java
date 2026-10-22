package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.SpriteRenderer;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplaySaveService;

public final class SpellPracticeEndScreen {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_RETRY = 1;
    public static final int ACTION_EXIT = 2;

    private static final int SCENE_RETRY_ASK = 0;
    private static final int SCENE_REPLAY_ASK_SAVE = 1;
    private static final int SCENE_REPLAY_SLOT = 2;
    private static final int SCENE_REPLAY_SAVE = 3;
    private static final int SCENE_REPLAY_OK = 4;
    private static final int SCENE_REPLAY_FAIL = 5;

    private static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private int scene;
    private int cursor;

    private int replayCursor;
    private int replayAnimationTick;

    private final ReplaySlotListModel slots = new ReplaySlotListModel();

    public void enter(boolean won) {
        scene = SCENE_RETRY_ASK;
        // DoJa behavior: default cursor differs by win/loss.
        // - win: default "No"
        // - loss: default "Yes"
        cursor = won ? 1 : 0;
        replayCursor = 0;
        replayAnimationTick = 0;
    }

    public int update(int pressed) {
        replayAnimationTick++;
        if (scene == SCENE_RETRY_ASK) {
            if (UiInput.anyVertical(pressed)) {
                cursor = MenuCursor.toggleTwo(cursor);
                return ACTION_NONE;
            }
            if (UiInput.confirm(pressed)) {
                if (cursor == 0) {
                    return ACTION_RETRY;
                }
                // If replay recording is disabled (e.g. cheat mode), there is no pending replay to save.
                if (!ReplaySaveService.hasPending()) {
                    return ACTION_EXIT;
                }
                scene = SCENE_REPLAY_ASK_SAVE;
                cursor = 0;
            }
            return ACTION_NONE;
        }

        if (scene == SCENE_REPLAY_ASK_SAVE) {
            if (!ReplaySaveService.hasPending()) {
                return ACTION_EXIT;
            }
            if (UiInput.anyVertical(pressed)) {
                cursor = MenuCursor.toggleTwo(cursor);
                return ACTION_NONE;
            }
            if (UiInput.confirm(pressed)) {
                if (cursor == 0) {
                    return ACTION_EXIT;
                }
                enterReplaySlotScene();
            }
            if (UiInput.cancel(pressed)) {
                return ACTION_EXIT;
            }
            return ACTION_NONE;
        }

        if (scene == SCENE_REPLAY_SLOT) {
            if (UiInput.down(pressed)) {
                replayCursor = MenuCursor.move(replayCursor, 1, slots.size());
            } else if (UiInput.up(pressed)) {
                replayCursor = MenuCursor.move(replayCursor, -1, slots.size());
            } else if (UiInput.confirm(pressed)) {
                scene = SCENE_REPLAY_SAVE;
            } else if (UiInput.cancel(pressed)) {
                return ACTION_EXIT;
            }
        } else if (scene == SCENE_REPLAY_OK) {
            if (UiInput.confirm(pressed)) {
                enterReplaySlotScene();
            }
            if (UiInput.cancel(pressed)) {
                return ACTION_EXIT;
            }
        } else if (scene == SCENE_REPLAY_FAIL) {
            if (UiInput.dismiss(pressed)) {
                enterReplaySlotScene();
            }
        }

        if (scene == SCENE_REPLAY_SAVE) {
            boolean ok = ReplaySaveService.savePendingToSlot(replayCursor);
            scene = ok ? SCENE_REPLAY_OK : SCENE_REPLAY_FAIL;
        }

        return ACTION_NONE;
    }

    public int getLeftSoftkeyTextId() {
        return scene == SCENE_RETRY_ASK || scene == SCENE_REPLAY_SAVE ? -1 : TextId.SOFTKEY_BACK;
    }

    public void render(Graphics g, SpriteRenderer sprites) {
        if (g == null) {
            return;
        }

        if (scene == SCENE_RETRY_ASK || scene == SCENE_REPLAY_ASK_SAVE) {
            boolean retry = scene == SCENE_RETRY_ASK;
            // semi-transparent black background (DoJa style)
            UiDraw.fillRectAlpha(g, 0, 8, 194, 226, 0x000000, 128);
            UiDraw.drawString2(g, FONT, UiText.get(retry ? TextId.SPELL_PRACTICE_RETRY_ASK : TextId.STAGECLEAR_ASK_SAVE_REPLAY),
                    97, 105, 1, 0xFF0000, 0x770000);
            String s0 = UiText.get(retry ? TextId.SPELL_PRACTICE_RETRY_YES : TextId.STAGECLEAR_SAVE_NO);
            String s1 = UiText.get(retry ? TextId.SPELL_PRACTICE_RETRY_NO : TextId.STAGECLEAR_SAVE_YES);
            if (cursor == 0) {
                UiDraw.drawString2(g, FONT, s0, 97, 130, 1, 0xFFFFFF, 0x777777);
                UiDraw.drawString2(g, FONT, s1, 97, 150, 1, 0x777777, 0x333333);
            } else {
                UiDraw.drawString2(g, FONT, s0, 97, 130, 1, 0x777777, 0x333333);
                UiDraw.drawString2(g, FONT, s1, 97, 150, 1, 0xFFFFFF, 0x777777);
            }
            return;
        }

        renderReplayOverlay(g, sprites);
    }

    private void enterReplaySlotScene() {
        scene = SCENE_REPLAY_SLOT;

        replayCursor = slots.clampCursor(replayCursor);
        slots.reload();
    }

    private void renderReplayOverlay(Graphics g, SpriteRenderer sprites) {
        boolean showList = (scene == SCENE_REPLAY_SLOT || scene == SCENE_REPLAY_OK || scene == SCENE_REPLAY_FAIL);
        if (!showList) {
            return;
        }

        ReplayUi.renderBackground(g, sprites);
        ReplayUi.renderSlotListCompact(g, FONT, replayCursor, slots,
                UiText.get(TextId.COMMON_SLOT_PREFIX), replayAnimationTick);

        if (scene == SCENE_REPLAY_OK || scene == SCENE_REPLAY_FAIL) {
            ReplayUi.renderSaveResultBox(g, FONT, scene == SCENE_REPLAY_OK, 104);
        }
    }

}
