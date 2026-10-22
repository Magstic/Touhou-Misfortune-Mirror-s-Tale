package touhou.battle;

import touhou.Player;
import touhou.SpriteCatalog;
import touhou.SpriteSheetCatalog;
import touhou.stage.BossController;
import touhou.stage.StageRuntime;

/** Pure layout and resource-selection rules for the 240x240 battle screen. */
public final class BattleRenderRules {
    public static final int LOGICAL_WIDTH = 240;
    public static final int LOGICAL_HEIGHT = 240;
    public static final int PLAY_X = 0;
    public static final int PLAY_Y = 8;
    public static final int PLAY_WIDTH = 194;
    public static final int PLAY_HEIGHT = 226;

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

    public static int bombPortraitSprite(int character) {
        return (character >= Player.REIMU && character <= Player.ALICE) ? SpriteSheetCatalog.BOMB_PORTRAIT_BASE + character : -1;
    }

    public static int bombOverlayX(int character, int bombAge) {
        if (character == Player.REIMU) {
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
            case StageRuntime.STAGE_1:
            case StageRuntime.STAGE_2:
            case StageRuntime.STAGE_3:
                return 45;
            case StageRuntime.STAGE_4:
                return 92;
            case StageRuntime.STAGE_5:
                return 96;
            case StageRuntime.STAGE_6:
                return 106;
            case StageRuntime.STAGE_EXTRA:
                return (phase & 3) == 0 ? 114 : 127 + (phase & 3);
            default:
                return 45;
        }
    }

    public static int backgroundOverlayImage(int stage, int phase) {
        switch (stage) {
            case StageRuntime.STAGE_2:
                return 59;
            case StageRuntime.STAGE_4:
                return 83;
            case StageRuntime.STAGE_5:
                return 98;
            case StageRuntime.STAGE_EXTRA:
                return (phase & 3) == 0 ? 115 : 130 + (phase & 3);
            default:
                return -1;
        }
    }

    public static int backgroundSecondaryImage(int stage) {
        return (stage == StageRuntime.STAGE_5) ? 97 : -1;
    }

    /** Sprite ID, logical destination X/Y and text color. */
    public static boolean dialoguePortrait(SpriteCatalog catalog, int type, int talkCounter, int[] out) {
        if (out == null || out.length < 4) {
            throw new IllegalArgumentException("Portrait output size");
        }
        if (type < 0 || type > 12) {
            return false;
        }
        int id = SpriteSheetCatalog.DIALOGUE_BASE + type;
        out[0] = id;
        out[1] = type <= 2 ? -talkCounter : PLAY_WIDTH - catalog.getW(id) + talkCounter;
        out[2] = type <= 2 ? 110 : PLAY_Y + PLAY_HEIGHT - catalog.getH(id);
        out[3] = PORTRAIT_COLORS[type];
        return true;
    }

    private static final int[] PORTRAIT_COLORS = new int[] {
        16731983, 10461087, 8355839, 16744319, 12566463, 5197823, 5197823,
        14614751, 16740207, 4161455, 8355711, 4145151, 10461183
    };

    public static int bossSpellCutInSprite(int stage, int gameMode, int spellStep) {
        switch (stage) {
            case StageRuntime.STAGE_1:
                return SpriteSheetCatalog.SUNNY_CUTIN;
            case StageRuntime.STAGE_2:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE;
            case StageRuntime.STAGE_3:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE + 1;
            case StageRuntime.STAGE_4:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE + ((gameMode == BattleSessionConfig.MODE_SPELL_PRACTICE && spellStep == BossController.LAST_WORD_STEP) ? 3 : 2);
            case StageRuntime.STAGE_5:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE + 4;
            case StageRuntime.STAGE_6:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE + 5;
            case StageRuntime.STAGE_EXTRA:
                return SpriteSheetCatalog.SINGLE_CUTIN_BASE + 6;
            default:
                throw new IllegalArgumentException("stage");
        }
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
