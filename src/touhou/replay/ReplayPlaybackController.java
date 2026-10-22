package touhou.replay;

import touhou.battle.BattleSessionConfig;

public final class ReplayPlaybackController {
    public static final class FrameInput {
        public int keys;
        public int pressed;
        public int fireActionPressed;
    }

    private boolean active;
    private int framePos;
    private byte[] frames;

    private ReplayHeader header;
    private final FrameInput frameInput = new FrameInput();

    public boolean isActive() {
        return active;
    }

    public int getFramePos() {
        return framePos;
    }

    public void stop() {
        active = false;
        framePos = 0;
        frames = null;
        header = null;
    }

    public ReplaySlotData startFromSlot(int slot, boolean bossOnly) {
        ReplaySlotData data = ReplaySlotStore.loadSlot(slot);
        if (data == null || (bossOnly && (data.header.getBossStartFrame() < 0 || data.header.getMode() == BattleSessionConfig.MODE_SPELL_PRACTICE))) {
            stop();
            return null;
        }
        frames = ReplayCodec.decode(data.fullInput);
        if (frames == null) {
            stop();
            return null;
        }

        active = true;
        framePos = 0;
        header = data.header;
        return data;
    }

    public int getRecordedDialoguePageCount(int encounterIndex) {
        return active ? header.getDialoguePageCountAt(encounterIndex) : -1;
    }

    public boolean wantsQuitToMenu(int pressed) {
        return active && ((pressed & javax.microedition.lcdui.game.GameCanvas.GAME_A_PRESSED) != 0);
    }

    public FrameInput nextFrame() {
        if (!active || framePos >= ReplayCodec.FRAME_COUNT) {
            return null;
        }

        byte b = frames[framePos++];

        FrameInput out = frameInput;

        int ks = ReplayKeyCodec.decodeToKeyState(b);
        out.keys = ks & (javax.microedition.lcdui.game.GameCanvas.UP_PRESSED
                | javax.microedition.lcdui.game.GameCanvas.DOWN_PRESSED
                | javax.microedition.lcdui.game.GameCanvas.LEFT_PRESSED
                | javax.microedition.lcdui.game.GameCanvas.RIGHT_PRESSED);

        int p = 0;
        if (ReplayKeyCodec.isToggleShot(b)) {
            p |= javax.microedition.lcdui.game.GameCanvas.FIRE_PRESSED;
        }
        if (ReplayKeyCodec.isToggleSlow(b)) {
            p |= javax.microedition.lcdui.game.GameCanvas.GAME_B_PRESSED;
        }
        if (ReplayKeyCodec.isBomb(b)) {
            p |= javax.microedition.lcdui.game.GameCanvas.GAME_C_PRESSED;
        }
        out.pressed = p;

        out.fireActionPressed = ReplayKeyCodec.isFireAction(b)
                ? javax.microedition.lcdui.game.GameCanvas.FIRE_PRESSED
                : 0;

        return out;
    }
}
