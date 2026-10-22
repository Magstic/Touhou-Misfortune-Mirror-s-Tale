package touhou.battle;

import touhou.BgmCatalog;

/** Immutable spell-practice metadata shared by UI, battle setup and replay. */
public final class SpellPracticeCatalog {
    private static final int STAGE = 0;
    private static final int DIFFICULTY = 1;
    private static final int ENEMY = 2;
    private static final int BOSS_STEP = 3;
    private static final int LAST_WORD_START = 107;

    private static final int[][] ENTRIES = new int[][] {
        { 0, 2, 130, 8 }, { 0, 3, 130, 8 },
        { 0, 0, 108, 19 }, { 0, 1, 108, 19 }, { 0, 2, 108, 19 }, { 0, 3, 108, 19 },
        { 0, 0, 110, 10 }, { 0, 1, 110, 10 }, { 0, 2, 110, 10 }, { 0, 3, 110, 10 },
        { 0, 0, 109, 13 }, { 0, 1, 109, 13 }, { 0, 2, 109, 13 }, { 0, 3, 109, 13 },
        { 0, 0, 111, 16 }, { 0, 1, 111, 16 }, { 0, 2, 111, 16 }, { 0, 3, 111, 16 },
        { 1, 0, 112, 10 }, { 1, 1, 112, 10 }, { 1, 2, 112, 10 }, { 1, 3, 112, 10 },
        { 1, 0, 112, 13 }, { 1, 1, 112, 13 }, { 1, 2, 112, 13 }, { 1, 3, 112, 13 },
        { 1, 0, 112, 16 }, { 1, 1, 112, 16 }, { 1, 2, 112, 16 }, { 1, 3, 112, 16 },
        { 2, 0, 132, 8 }, { 2, 1, 132, 8 }, { 2, 2, 132, 8 }, { 2, 3, 132, 8 },
        { 2, 0, 113, 10 }, { 2, 1, 113, 10 }, { 2, 2, 113, 10 }, { 2, 3, 113, 10 },
        { 2, 0, 113, 13 }, { 2, 1, 113, 13 }, { 2, 2, 113, 13 }, { 2, 3, 113, 13 },
        { 2, 0, 113, 16 }, { 2, 1, 113, 16 }, { 2, 2, 113, 16 }, { 2, 3, 113, 16 },
        { 3, 0, 114, 10 }, { 3, 1, 114, 10 }, { 3, 2, 114, 10 }, { 3, 3, 114, 10 },
        { 3, 0, 114, 13 }, { 3, 1, 114, 13 }, { 3, 2, 114, 13 }, { 3, 3, 114, 13 },
        { 3, 0, 114, 16 }, { 3, 1, 114, 16 }, { 3, 2, 114, 16 }, { 3, 3, 114, 16 },
        { 3, 0, 114, 19 }, { 3, 1, 114, 19 }, { 3, 2, 114, 19 }, { 3, 3, 114, 19 },
        { 4, 0, 115, 10 }, { 4, 1, 115, 10 }, { 4, 2, 115, 10 }, { 4, 3, 115, 10 },
        { 4, 0, 115, 13 }, { 4, 1, 115, 13 }, { 4, 2, 115, 13 }, { 4, 3, 115, 13 },
        { 4, 0, 115, 16 }, { 4, 1, 115, 16 }, { 4, 2, 115, 16 }, { 4, 3, 115, 16 },
        { 4, 0, 115, 19 }, { 4, 1, 115, 19 }, { 4, 2, 115, 19 }, { 4, 3, 115, 19 },
        { 5, 0, 116, 10 }, { 5, 1, 116, 10 }, { 5, 2, 116, 10 }, { 5, 3, 116, 10 },
        { 5, 0, 116, 13 }, { 5, 1, 116, 13 }, { 5, 2, 116, 13 }, { 5, 3, 116, 13 },
        { 5, 0, 116, 16 }, { 5, 1, 116, 16 }, { 5, 2, 116, 16 }, { 5, 3, 116, 16 },
        { 5, 0, 116, 19 }, { 5, 1, 116, 19 }, { 5, 2, 116, 19 }, { 5, 3, 116, 19 },
        { 5, 0, 116, 22 }, { 5, 1, 116, 22 }, { 5, 2, 116, 22 }, { 5, 3, 116, 22 },
        { 6, 4, 117, 10 }, { 6, 4, 117, 13 }, { 6, 4, 117, 16 }, { 6, 4, 117, 19 },
        { 6, 4, 117, 22 }, { 6, 4, 117, 25 }, { 6, 4, 117, 28 }, { 6, 4, 117, 31 },
        { 6, 4, 117, 34 },
        { 0, 5, 108, 37 }, { 0, 5, 110, 40 }, { 0, 5, 109, 43 },
        { 1, 5, 112, 37 }, { 2, 5, 113, 37 }, { 3, 5, 114, 37 },
        { 4, 5, 115, 37 }, { 5, 5, 116, 37 }, { 6, 5, 117, 37 }
    };

    private SpellPracticeCatalog() {
    }

    public static int size() {
        return ENTRIES.length;
    }

    public static boolean isValid(int spellId) {
        return spellId >= 0 && spellId < ENTRIES.length;
    }

    public static int getStage(int spellId) {
        return entry(spellId)[STAGE];
    }

    public static int getDifficulty(int spellId) {
        return entry(spellId)[DIFFICULTY];
    }

    public static int getEnemyId(int spellId) {
        return entry(spellId)[ENEMY];
    }

    public static int getBossStep(int spellId) {
        return entry(spellId)[BOSS_STEP];
    }

    public static int getBossBgmTrack(int spellId) {
        int stage = getStage(spellId);
        return spellId >= LAST_WORD_START ? BgmCatalog.LAST_WORD : BgmCatalog.FIRST_BOSS_TRACK + (stage * 2);
    }

    private static int[] entry(int spellId) {
        if (!isValid(spellId)) {
            throw new IllegalArgumentException("Invalid spell practice id: " + spellId);
        }
        return ENTRIES[spellId];
    }
}
