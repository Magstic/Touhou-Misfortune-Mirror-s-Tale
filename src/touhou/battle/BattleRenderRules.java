package touhou.battle;

/** Pure layout and resource-selection rules for the 240x240 battle screen. */
public final class BattleRenderRules {
    public static final int LOGICAL_WIDTH = 240;
    public static final int LOGICAL_HEIGHT = 240;
    public static final int PLAY_X = 0;
    public static final int PLAY_Y = 8;
    public static final int PLAY_WIDTH = 194;
    public static final int PLAY_HEIGHT = 226;

    public static final int CUT_IN_PHASE_FADE_IN = 1;
    public static final int CUT_IN_PHASE_HOLD = 2;
    public static final int CUT_IN_PHASE_FADE_OUT = 3;
    public static final int CUT_IN_ALPHA_MAX = 128;

    private static final int CUT_IN_FADE_IN_FRAMES = 4;
    private static final int CUT_IN_FADE_OUT_FRAMES = 8;

    private BattleRenderRules() {
    }

    public static int fixedToScreen(int origin, int fixedPosition) {
        return origin + (fixedPosition >> 16);
    }

    public static int endingFadeAlpha(int phase) {
        return 128 + (clamp(phase, 0, 15) << 2);
    }

    public static int screenFadeAlpha(int counter) {
        if (counter <= 0) {
            return 0;
        }
        if (counter >= 10) {
            return 255;
        }
        return clamp(128 + (128 * counter) / 10, 0, 255);
    }

    public static int bossSpellCutInAlpha(int phase, boolean dynamic, int elapsedFrames) {
        if (phase == 0) {
            return 0;
        }
        if (!dynamic) {
            return CUT_IN_ALPHA_MAX;
        }
        int elapsed = (elapsedFrames < 0) ? 0 : elapsedFrames;
        int alpha;
        if (phase == CUT_IN_PHASE_FADE_IN) {
            alpha = (CUT_IN_ALPHA_MAX * elapsed) / CUT_IN_FADE_IN_FRAMES;
        } else if (phase == CUT_IN_PHASE_FADE_OUT) {
            alpha = CUT_IN_ALPHA_MAX - ((CUT_IN_ALPHA_MAX * elapsed) / CUT_IN_FADE_OUT_FRAMES);
        } else {
            alpha = CUT_IN_ALPHA_MAX;
        }
        alpha = clamp(alpha, 0, CUT_IN_ALPHA_MAX);
        alpha = (alpha + 15) & ~15;
        return clamp(alpha, 0, 255);
    }

    public static int bossSpellCutInImage(int stage, int gameMode, int spellPracticeBossStep) {
        if (stage == 3 && gameMode == BattleSessionConfig.MODE_SPELL_PRACTICE && spellPracticeBossStep == 37) {
            return 58;
        }
        switch (stage) {
            case 0:
                return 30;
            case 1:
                return 40;
            case 2:
                return 51;
            case 3:
                return 56;
            case 4:
                return 65;
            case 5:
                return 69;
            case 6:
                return 76;
            default:
                return -1;
        }
    }

    public static int bombOverlayImage(int character) {
        return (character >= 0 && character <= 2) ? 12 + character : -1;
    }

    public static int safeCharacterIndex(int character) {
        return clamp(character, 0, 2);
    }

    public static int bombOverlayX(int character, int bombAge) {
        if (character == 0) {
            return 0;
        }
        return -((70 + 6 * bombAge) >> 5);
    }

    public static int bombOverlayY(int bombAge) {
        return PLAY_Y + ((70 + 6 * bombAge) >> 5);
    }

    public static int bombTitleY(int bombAge) {
        if (bombAge < 5) {
            return 166;
        }
        if (bombAge < 15) {
            return 166 + (bombAge - 5) * 6;
        }
        return 226;
    }

    public static int backgroundBaseImage(int stage, int phase) {
        switch (stage) {
            case 0:
            case 1:
            case 2:
                return 25;
            case 3:
                return 59;
            case 4:
                return 62;
            case 5:
                return 70;
            case 6:
                return 77 + (phase & 3);
            default:
                return 25;
        }
    }

    public static int backgroundOverlayImage(int stage, int phase) {
        switch (stage) {
            case 1:
                return 32;
            case 3:
                return 54;
            case 4:
                return 64;
            case 6:
                return 81 + (phase & 3);
            default:
                return -1;
        }
    }

    public static int backgroundSecondaryImage(int stage) {
        return (stage == 4) ? 63 : -1;
    }

    /** Writes image, source x/y, width/height, destination x/y and text color. */
    public static boolean dialoguePortrait(int type, int talkCounter, int[] out) {
        if (out == null || out.length < 8) {
            throw new IllegalArgumentException("Dialogue portrait output must contain 8 values");
        }
        int image;
        int sourceX;
        int sourceY;
        int width;
        int height;
        int color;
        switch (type) {
            case 0:
                image = 8; sourceX = 0; sourceY = 72; width = 71; height = 124; color = 16731983; break;
            case 1:
                image = 9; sourceX = 0; sourceY = 72; width = 76; height = 124; color = 10461087; break;
            case 2:
                image = 10; sourceX = 0; sourceY = 72; width = 76; height = 124; color = 8355839; break;
            case 3:
                image = 28; sourceX = 0; sourceY = 7; width = 76; height = 117; color = 16744319; break;
            case 4:
                image = 28; sourceX = 152; sourceY = 5; width = 76; height = 119; color = 12566463; break;
            case 5:
            case 6:
                image = 28; sourceX = 76; sourceY = 10; width = 76; height = 114; color = 5197823; break;
            case 7:
                image = 39; sourceX = 0; sourceY = 0; width = 75; height = 123; color = 14614751; break;
            case 8:
                image = 50; sourceX = 1; sourceY = 3; width = 75; height = 121; color = 16740207; break;
            case 9:
                image = 53; sourceX = 0; sourceY = 0; width = 76; height = 124; color = 4161455; break;
            case 10:
                image = 60; sourceX = 0; sourceY = 0; width = 72; height = 124; color = 8355711; break;
            case 11:
                image = 72; sourceX = 0; sourceY = 0; width = 76; height = 124; color = 4145151; break;
            case 12:
                image = 74; sourceX = 0; sourceY = 0; width = 75; height = 123; color = 10461183; break;
            default:
                return false;
        }
        out[0] = image;
        out[1] = sourceX;
        out[2] = sourceY;
        out[3] = width;
        out[4] = height;
        if (type <= 2) {
            out[5] = -talkCounter;
            out[6] = 110;
        } else {
            out[5] = PLAY_WIDTH - width + talkCounter;
            out[6] = PLAY_Y + PLAY_HEIGHT - height;
        }
        out[7] = color;
        return true;
    }

    /** Writes destination x/y, source x/y and width/height after source-image clipping. */
    public static boolean clipSourceRegion(int x, int y, int sourceX, int sourceY,
            int width, int height, int anchorX, int anchorY, int scale,
            int imageWidth, int imageHeight, int[] out) {
        if (out == null || out.length < 6) {
            throw new IllegalArgumentException("Clipped region output must contain 6 values");
        }
        if (scale <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return false;
        }
        sourceX *= scale;
        sourceY *= scale;
        width *= scale;
        height *= scale;
        anchorX *= scale;
        anchorY *= scale;
        int destinationX = x - anchorX;
        int destinationY = y - anchorY;
        if (sourceX < 0) {
            int cut = -sourceX;
            sourceX = 0;
            destinationX += cut;
            width -= cut;
        }
        if (sourceY < 0) {
            int cut = -sourceY;
            sourceY = 0;
            destinationY += cut;
            height -= cut;
        }
        if (width <= 0 || height <= 0 || sourceX >= imageWidth || sourceY >= imageHeight) {
            return false;
        }
        if ((long) sourceX + width > imageWidth) {
            width = imageWidth - sourceX;
        }
        if ((long) sourceY + height > imageHeight) {
            height = imageHeight - sourceY;
        }
        if (width <= 0 || height <= 0) {
            return false;
        }
        out[0] = destinationX;
        out[1] = destinationY;
        out[2] = sourceX;
        out[3] = sourceY;
        out[4] = width;
        out[5] = height;
        return true;
    }

    /** Crops a destination/source rectangle to the playfield. */
    public static boolean clipToPlayfield(int destinationX, int destinationY, int sourceX, int sourceY,
            int width, int height, int[] out) {
        if (out == null || out.length < 6) {
            throw new IllegalArgumentException("Clipped region output must contain 6 values");
        }
        if (width <= 0 || height <= 0) {
            return false;
        }
        long left = destinationX;
        long top = destinationY;
        long right = left + width;
        long bottom = top + height;
        long clippedLeft = (left < PLAY_X) ? PLAY_X : left;
        long clippedTop = (top < PLAY_Y) ? PLAY_Y : top;
        long playRight = PLAY_X + PLAY_WIDTH;
        long playBottom = PLAY_Y + PLAY_HEIGHT;
        long clippedRight = (right > playRight) ? playRight : right;
        long clippedBottom = (bottom > playBottom) ? playBottom : bottom;
        if (clippedLeft >= clippedRight || clippedTop >= clippedBottom) {
            return false;
        }
        long clippedSourceX = (long) sourceX + clippedLeft - left;
        long clippedSourceY = (long) sourceY + clippedTop - top;
        if (clippedSourceX < Integer.MIN_VALUE || clippedSourceX > Integer.MAX_VALUE
                || clippedSourceY < Integer.MIN_VALUE || clippedSourceY > Integer.MAX_VALUE) {
            return false;
        }
        out[0] = (int) clippedLeft;
        out[1] = (int) clippedTop;
        out[2] = (int) clippedSourceX;
        out[3] = (int) clippedSourceY;
        out[4] = (int) (clippedRight - clippedLeft);
        out[5] = (int) (clippedBottom - clippedTop);
        return true;
    }

    public static String dialogueLine(String text, int lineIndex) {
        if (text == null || lineIndex < 0) {
            return null;
        }
        int current = 0;
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i != text.length() && text.charAt(i) != '|') {
                continue;
            }
            if (current == lineIndex) {
                return text.substring(start, i);
            }
            current++;
            start = i + 1;
        }
        return null;
    }

    public static int digitCount(int value, int maxDigits) {
        if (maxDigits <= 0) {
            throw new IllegalArgumentException("Maximum digit count must be positive");
        }
        int count = 1;
        int threshold = 10;
        while (count < maxDigits && value >= threshold) {
            count++;
            if (threshold > 100000000) {
                break;
            }
            threshold *= 10;
        }
        return count;
    }

    public static void buildStableRanks(int[] ranks, int[] keys, int count) {
        if (ranks == null || keys == null || count < 0 || count > ranks.length || count > keys.length) {
            throw new IllegalArgumentException("Invalid rank buffers or count");
        }
        for (int i = 0; i < count; i++) {
            int rank = 0;
            for (int j = 0; j < count; j++) {
                if (keys[j] < keys[i] || (keys[j] == keys[i] && j < i)) {
                    rank++;
                }
            }
            ranks[i] = rank;
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
