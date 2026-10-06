package touhou.battle;

/** Update-owned transient visual state. Rendering reads this object without mutating it. */
public final class BattleVisualState {
    private static final int BLIND_ALPHA_MAX = 128;
    private static final int BLIND_FADE_OUT_FRAMES = 4;

    private int flash;
    private int screenFade;
    private int blindAlpha;
    private int blindSpellId = -1;

    public void reset() {
        flash = 0;
        screenFade = 0;
        blindAlpha = 0;
        blindSpellId = -1;
    }

    public void beginFrame() {
        flash = 0;
        if (screenFade > 0) {
            screenFade--;
        }
    }

    public void endFrame(int stage, boolean bossMode, int spellId, int bossFrame, boolean blindFadeEnabled) {
        advanceBlind(stage, bossMode, spellId, bossFrame, blindFadeEnabled);
    }

    public void setFlash(int value) {
        flash = value;
    }

    public int getFlash() {
        return flash;
    }

    public void setScreenFade(int value) {
        screenFade = (value < 0) ? 0 : value;
    }

    public int getScreenFade() {
        return screenFade;
    }

    public int getBlindAlpha() {
        return blindAlpha;
    }

    public int getBlindSpellId() {
        return blindSpellId;
    }

    private void advanceBlind(int stage, boolean bossMode, int spellId, int bossFrame, boolean fadeEnabled) {
        if (stage != 1) {
            blindAlpha = 0;
            blindSpellId = -1;
            return;
        }

        boolean active = bossMode && spellId >= 26 && spellId <= 29 && (bossFrame == 17 || bossFrame == 18);
        if (active) {
            blindSpellId = spellId;
            if (!fadeEnabled) {
                blindAlpha = BLIND_ALPHA_MAX;
            } else if (blindAlpha < BLIND_ALPHA_MAX) {
                blindAlpha += 2;
                if (blindAlpha > BLIND_ALPHA_MAX) {
                    blindAlpha = BLIND_ALPHA_MAX;
                }
            }
            return;
        }

        if (!fadeEnabled) {
            blindAlpha = 0;
            blindSpellId = -1;
            return;
        }
        if (blindAlpha > 0) {
            blindAlpha -= BLIND_ALPHA_MAX / BLIND_FADE_OUT_FRAMES;
            if (blindAlpha < 0) {
                blindAlpha = 0;
            }
        }
        if (blindAlpha == 0) {
            blindSpellId = -1;
        }
    }

}
