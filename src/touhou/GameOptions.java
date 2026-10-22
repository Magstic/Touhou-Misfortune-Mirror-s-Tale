package touhou;

public final class GameOptions {
    private static final int SP_OFFSET = 3;

    public static final int SIZE = 16;

    public static final int IDX_SE_MASK = 12;
    public static final int IDX_SE_MASK_HI = 13;

    public static final int IDX_CHEAT_DISABLE_BOSS_BOMB_SHIELD = 14;

    public static final int IDX_CHEAT_DISABLE_ITEM_COLLECT_LIMIT = 15;

    public static final int IDX_LANGUAGE = 11;

    public static final int LANG_JA = 0;
    public static final int LANG_EN = 1;
    public static final int LANG_ZHT = 2;

    // Rendering and debug switches packed into opt[3].
    private static final int IDX_RENDER_FLAGS = 3;
    private static final int ALPHA_MASK = 0x07;
    private static final int ALPHA_CLOSE_UP = 0x03;
    private static final int ALPHA_PLAYER_SHOT = 0x04;

    // Compatibility switches packed into opt[10].
    private static final int IDX_RENDER_MISC_FLAGS = 10;
    private static final int RENDER_HEAVY_BG_OFF = 0x01;
    private static final int RENDER_BLIND_MASK_NO_FADE = 0x02;
    private static final int RENDER_HIT_SPARK_OFF = 0x04;
    private static final int HEAVY_BGM_OFF = 0x08;
    private static final int RENDER_MISC_MASK = RENDER_HEAVY_BG_OFF | RENDER_BLIND_MASK_NO_FADE | RENDER_HIT_SPARK_OFF | HEAVY_BGM_OFF;

    // Debug overlay switches packed into opt[3].
    private static final int DEBUG_MASK = 0x38;
    private static final int DEBUG_FPS = 0x08;
    private static final int DEBUG_PERF = 0x10;
    private static final int DEBUG_RESOURCE = 0x20;

    private GameOptions() {
    }

    // Heuristic: emulator heaps are typically much larger than real devices.
    public static boolean isProbablyEmulator() {
        try {
            Runtime rt = Runtime.getRuntime();
            long total = rt.totalMemory();
            return total >= (8L * 1024L * 1024L);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isCloseUpAlphaEnabled(int[] opt) {
        return (alphaFlags(opt) & ALPHA_CLOSE_UP) != 0;
    }

    public static boolean isPlayerShotAlphaEnabled(int[] opt) {
        return (alphaFlags(opt) & ALPHA_PLAYER_SHOT) != 0;
    }

    public static boolean isHeavyBgEnabled(int[] opt) {
        return (opt[IDX_RENDER_MISC_FLAGS] & RENDER_HEAVY_BG_OFF) == 0;
    }

    public static boolean isHeavyBgmEnabled(int[] opt) {
        return (opt[IDX_RENDER_MISC_FLAGS] & HEAVY_BGM_OFF) == 0;
    }

    public static boolean isBlindMaskFadeEnabled(int[] opt) {
        return (opt[IDX_RENDER_MISC_FLAGS] & RENDER_BLIND_MASK_NO_FADE) == 0;
    }

    public static boolean isHitSparkEnabled(int[] opt) {
        return (opt[IDX_RENDER_MISC_FLAGS] & RENDER_HIT_SPARK_OFF) == 0;
    }

    public static boolean isDebugFpsEnabled(int[] opt) {
        return (debugFlags(opt) & DEBUG_FPS) != 0;
    }

    public static boolean isDebugPerfEnabled(int[] opt) {
        return (debugFlags(opt) & DEBUG_PERF) != 0;
    }

    public static boolean isDebugResourceEnabled(int[] opt) {
        return (debugFlags(opt) & DEBUG_RESOURCE) != 0;
    }

    public static void toggleCloseUpAlpha(int[] opt) {
        opt[IDX_RENDER_FLAGS] = isCloseUpAlphaEnabled(opt)
                ? opt[IDX_RENDER_FLAGS] & ~ALPHA_CLOSE_UP : opt[IDX_RENDER_FLAGS] | ALPHA_CLOSE_UP;
    }

    public static void togglePlayerShotAlpha(int[] opt) {
        toggleAlphaBit(opt, ALPHA_PLAYER_SHOT);
    }

    public static void toggleHeavyBg(int[] opt) {
        opt[IDX_RENDER_MISC_FLAGS] ^= RENDER_HEAVY_BG_OFF;
    }

    public static void toggleHeavyBgm(int[] opt) {
        opt[IDX_RENDER_MISC_FLAGS] ^= HEAVY_BGM_OFF;
    }

    public static void toggleBlindMaskFade(int[] opt) {
        opt[IDX_RENDER_MISC_FLAGS] ^= RENDER_BLIND_MASK_NO_FADE;
    }

    public static void toggleHitSpark(int[] opt) {
        opt[IDX_RENDER_MISC_FLAGS] ^= RENDER_HIT_SPARK_OFF;
    }

    public static void toggleDebugFps(int[] opt) {
        toggleDebugBit(opt, DEBUG_FPS);
    }

    public static void toggleDebugPerf(int[] opt) {
        toggleDebugBit(opt, DEBUG_PERF);
    }

    public static void toggleDebugResource(int[] opt) {
        toggleDebugBit(opt, DEBUG_RESOURCE);
    }

    private static void toggleAlphaBit(int[] opt, int bit) {
        opt[IDX_RENDER_FLAGS] ^= bit;
    }

    private static void toggleDebugBit(int[] opt, int bit) {
        opt[IDX_RENDER_FLAGS] ^= bit;
    }

    private static int alphaFlags(int[] opt) {
        return opt[IDX_RENDER_FLAGS] & ALPHA_MASK;
    }

    private static int debugFlags(int[] opt) {
        return opt[IDX_RENDER_FLAGS] & DEBUG_MASK;
    }

    public static int[] defaults() {
        int[] opt = new int[SIZE];
        boolean emu = isProbablyEmulator();
        opt[0] = 3;
        opt[1] = 70;
        opt[2] = 100;
        opt[IDX_RENDER_FLAGS] = ALPHA_MASK;
        opt[4] = 0;
        opt[5] = 0;
        opt[6] = 10;
        opt[7] = 11;
        opt[8] = 12;
        // HeavyBG: always ON; Mistia Fade: ON for emulator, OFF for real device.
        // Hit Spark: always ON.
        opt[IDX_RENDER_MISC_FLAGS] = emu ? 0 : RENDER_BLIND_MASK_NO_FADE;
        opt[IDX_LANGUAGE] = LANG_JA;

        opt[IDX_CHEAT_DISABLE_BOSS_BOMB_SHIELD] = 0;

        opt[IDX_CHEAT_DISABLE_ITEM_COLLECT_LIMIT] = 0;

        // SE_SHOT (bit 10) defaults to OFF even on emulator to avoid noisy rapid-fire sound.
        int seMask = emu ? 0x3BFF : 0x0000;
        opt[IDX_SE_MASK] = seMask & 0xFF;
        opt[IDX_SE_MASK_HI] = (seMask >> 8) & 0xFF;
        return opt;
    }

    public static int[] load() {
        int[] opt = loadFromSp();
        if (opt != null) {
            return opt;
        }
        int[] def = defaults();
        saveToSp(def);
        return def;
    }

    public static void save(int[] opt) {
        if (!isValid(opt)) {
            throw new IllegalArgumentException("Invalid options");
        }
        saveToSp(opt);
    }

    public static boolean isCheatDisableBossBombShield(int[] opt) {
        return opt[IDX_CHEAT_DISABLE_BOSS_BOMB_SHIELD] != 0;
    }

    public static boolean isCheatDisableItemCollectLimit(int[] opt) {
        return opt[IDX_CHEAT_DISABLE_ITEM_COLLECT_LIMIT] != 0;
    }

    private static byte[] toBytes(int[] opt) {
        byte[] data = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            data[i] = (byte) opt[i];
        }
        return data;
    }

    private static int[] loadFromSp() {
        byte[] data = SpStore.read(SP_OFFSET, SIZE);
        boolean empty = true;
        int[] opt = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            opt[i] = data[i] & 0xFF;
            if (data[i] != 0) {
                empty = false;
            }
        }
        if (empty) {
            return null; // The untouched initialization block, not an I/O failure.
        }
        if (!isValid(opt)) {
            throw new StorageException("Invalid stored game options");
        }
        return opt;
    }

    private static void saveToSp(int[] opt) {
        SpStore.write(SP_OFFSET, toBytes(opt));
    }

    private static boolean isValid(int[] opt) {
        if (opt == null || opt.length != SIZE) {
            return false;
        }
        for (int i = 0; i < SIZE; i++) {
            if (opt[i] < 0 || opt[i] > 255) {
                return false;
            }
        }
        if (opt[0] < 1 || opt[0] > 6) {
            return false;
        }
        if (opt[1] < 0 || opt[1] > 100) {
            return false;
        }
        if (opt[2] < 0 || opt[2] > 100) {
            return false;
        }
        if ((opt[IDX_RENDER_FLAGS] & ~(ALPHA_MASK | DEBUG_MASK)) != 0) {
            return false;
        }
        if (opt[6] < 0 || opt[6] > 13) {
            return false;
        }
        if (opt[7] < 0 || opt[7] > 13) {
            return false;
        }
        if (opt[8] < 0 || opt[8] > 13) {
            return false;
        }
        if (opt[IDX_SE_MASK] < 0 || opt[IDX_SE_MASK] > 255) {
            return false;
        }
        if (opt[IDX_SE_MASK_HI] < 0 || opt[IDX_SE_MASK_HI] > 63) {
            return false;
        }
        if (opt[IDX_CHEAT_DISABLE_BOSS_BOMB_SHIELD] != 0 && opt[IDX_CHEAT_DISABLE_BOSS_BOMB_SHIELD] != 1) {
            return false;
        }
        if (opt[IDX_CHEAT_DISABLE_ITEM_COLLECT_LIMIT] != 0 && opt[IDX_CHEAT_DISABLE_ITEM_COLLECT_LIMIT] != 1) {
            return false;
        }
        if ((opt[IDX_RENDER_MISC_FLAGS] & ~RENDER_MISC_MASK) != 0) {
            return false;
        }
        if (opt[IDX_LANGUAGE] < 0 || opt[IDX_LANGUAGE] > LANG_ZHT) {
            return false;
        }
        return true;
    }
}
