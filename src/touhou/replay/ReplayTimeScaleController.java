package touhou.replay;

import javax.microedition.lcdui.game.GameCanvas;

public final class ReplayTimeScaleController {
    private static final int AUTO_FAST_FORWARD_STEPS = 16;
    private static final int FAST_FORWARD_STEPS = 8;

    private int slowCounter;
    private int autoFastForwardTargetFrame = -1;

    public void reset() {
        slowCounter = 0;
        autoFastForwardTargetFrame = -1;
    }

    public void startAutoFastForwardToFrame(int frameIndex) {
        autoFastForwardTargetFrame = frameIndex < 0 ? -1 : frameIndex;
    }

    // Returns how many replay simulation steps should run in this render frame.
    public int computeSteps(int liveKeys, ReplayPlaybackController playback) {
        if (playback == null || !playback.isActive()) {
            return 1;
        }

        int target = autoFastForwardTargetFrame;
        if (target >= 0) {
            int pos = playback.getFramePos();
            int remaining = target - pos;
            if (remaining > 0) {
                return Math.min(AUTO_FAST_FORWARD_STEPS, remaining);
            }

            autoFastForwardTargetFrame = -1;
        }

        if ((liveKeys & GameCanvas.RIGHT_PRESSED) != 0) {
            slowCounter = 0;
            return FAST_FORWARD_STEPS;
        }

        if ((liveKeys & GameCanvas.LEFT_PRESSED) != 0) {
            slowCounter++;
            return ((slowCounter & 1) == 0) ? 1 : 0;
        }

        slowCounter = 0;
        return 1;
    }
}
