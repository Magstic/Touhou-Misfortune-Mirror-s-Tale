package touhou;

public final class UnlockFlags {
    private static final int OFFSET_UFLAGS = 106;
    private static final int LEN_UFLAGS = 10;

    private static final int IDX_EXTRA_UNLOCKED = 0;
    private static final int IDX_FLAG1 = 1;
    private static final int IDX_ALICE_UNLOCKED = 2;
    private static final int IDX_SPELL_PRACTICE_UNLOCKED = 3;
    private static final int IDX_REPLAY_RMS_WARMED = 4;

    private static final byte[] flags = new byte[LEN_UFLAGS];
    private static boolean loaded;

    private UnlockFlags() {
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        byte[] data = SpStore.read(OFFSET_UFLAGS, LEN_UFLAGS);
        System.arraycopy(data, 0, flags, 0, LEN_UFLAGS);
        loaded = true;
    }

    private static void setFlag(int index, boolean value) {
        ensureLoaded();
        byte next = (byte) (value ? 1 : 0);
        if (flags[index] == next) {
            return;
        }
        byte[] data = new byte[LEN_UFLAGS];
        System.arraycopy(flags, 0, data, 0, LEN_UFLAGS);
        data[index] = next;
        SpStore.write(OFFSET_UFLAGS, data);
        flags[index] = next;
    }

    public static boolean isExtraUnlocked() {
        ensureLoaded();
        return flags[IDX_EXTRA_UNLOCKED] != 0;
    }

    public static void setExtraUnlocked(boolean v) {
        setFlag(IDX_EXTRA_UNLOCKED, v);
    }

    public static boolean isAliceUnlocked() {
        ensureLoaded();
        if (flags[IDX_ALICE_UNLOCKED] != 0) {
            return true;
        }
        if (qualifiesAliceFromSpellCaptures()) {
            setFlag(IDX_ALICE_UNLOCKED, true);
            return true;
        }
        return false;
    }

    public static void setAliceUnlocked(boolean v) {
        setFlag(IDX_ALICE_UNLOCKED, v);
    }

    private static boolean qualifiesAliceFromSpellCaptures() {
        // Unlock Alice when unique captured SpellCards >= 41.
        // Count is based on sc_bonuscnt (captured/bonus), summed across all units.
        return GameProgress.INSTANCE.countCapturedSpells() >= 41;
    }

    private static boolean qualifiesExtraOrNextExtra(int clearedStage, int gamemode, int startLevel, int remainingContinues, int runStartStage, boolean startPracBoss) {
        // Unlock condition:
        // - Story mode only (Game Start)
        // - Clear Stage 6
        // - Started from Stage 1 (no stage select)
        // - Not boss practice start
        // - Normal+ difficulty
        // - No continues used in this run
        if (clearedStage != 5) {
            return false;
        }
        if (gamemode != 0) {
            return false;
        }
        if (runStartStage != 0) {
            return false;
        }
        if (startPracBoss) {
            return false;
        }
        if (startLevel < 1) {
            return false;
        }
        if (remainingContinues != 3) {
            return false;
        }
        return true;
    }

    public static boolean qualifiesNextExtraOnStoryClear(int clearedStage, int gamemode, int startLevel, int remainingContinues, int runStartStage, boolean startPracBoss) {
        return qualifiesExtraOrNextExtra(clearedStage, gamemode, startLevel, remainingContinues, runStartStage, startPracBoss);
    }

    public static void tryUnlockExtraOnStoryClear(int clearedStage, int gamemode, int startLevel, int remainingContinues, int runStartStage, boolean startPracBoss) {
        if (!qualifiesExtraOrNextExtra(clearedStage, gamemode, startLevel, remainingContinues, runStartStage, startPracBoss)) {
            return;
        }
        setExtraUnlocked(true);
    }

    public static void tryUnlockAliceOnExtraClear(int clearedStage, int gamemode) {
        // PLUS: clear EX stage to unlock Alice.
        if (clearedStage != 6) {
            return;
        }
        if (gamemode != 1) {
            return;
        }
        setAliceUnlocked(true);
    }

    public static boolean isSpellPracticeUnlocked() {
        ensureLoaded();
        return flags[IDX_SPELL_PRACTICE_UNLOCKED] != 0;
    }

    // One-time startup warmup flag: indicates Replay-related RMS stores were initialized.
    public static boolean isReplayRmsWarmed() {
        ensureLoaded();
        return flags[IDX_REPLAY_RMS_WARMED] != 0;
    }

    public static void setReplayRmsWarmed(boolean v) {
        setFlag(IDX_REPLAY_RMS_WARMED, v);
    }

    public static void tryUnlockSpellPracticeOnAnyEnding() {
        // Original behavior: unlock SpellPractice after seeing any ending.
        setFlag(IDX_SPELL_PRACTICE_UNLOCKED, true);
    }

    public static void tryUnlockFlag1FromStageOp(int gamemode) {
        if (gamemode != 3) {
            setFlag(IDX_FLAG1, true);
        }
    }
}
