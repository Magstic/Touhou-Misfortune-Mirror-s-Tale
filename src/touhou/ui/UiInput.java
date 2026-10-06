package touhou.ui;

import javax.microedition.lcdui.game.GameCanvas;

import touhou.GameCore;

/** Semantic names for UI button edges. Gameplay input remains in InputSystem. */
public final class UiInput {
    private UiInput() {
    }

    public static boolean confirm(int pressed) {
        return (pressed & GameCanvas.FIRE_PRESSED) != 0;
    }

    public static boolean cancel(int pressed) {
        return (pressed & GameCanvas.GAME_A_PRESSED) != 0;
    }

    public static boolean secondary(int pressed) {
        return (pressed & GameCanvas.GAME_B_PRESSED) != 0;
    }

    public static boolean dismiss(int pressed) {
        return confirm(pressed) || cancel(pressed);
    }

    public static boolean up(int pressed) {
        return (pressed & GameCanvas.UP_PRESSED) != 0;
    }

    public static boolean down(int pressed) {
        return (pressed & GameCanvas.DOWN_PRESSED) != 0;
    }

    public static boolean left(int pressed) {
        return (pressed & GameCanvas.LEFT_PRESSED) != 0;
    }

    public static boolean right(int pressed) {
        return (pressed & GameCanvas.RIGHT_PRESSED) != 0;
    }

    public static boolean anyDirection(int pressed) {
        return up(pressed) || down(pressed) || left(pressed) || right(pressed);
    }

    public static boolean anyVertical(int pressed) {
        return up(pressed) || down(pressed);
    }

    public static boolean replaySave(int pressed) {
        return (pressed & GameCore.KEY_POUND_PRESSED) != 0;
    }
}
