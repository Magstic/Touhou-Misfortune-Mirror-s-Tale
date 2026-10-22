package touhou;

import touhou.battle.BattleSessionConfig;

/** Sole owner of persistent progress. Commands serialize read/modify/write and publish after success. */
public final class GameProgress {

    public static final int UNIT_COUNT = 6;
    public static final int SPELL_COUNT = 116;

    private static final int OFFSET_USERDATA = 20;
    private static final int LEN_USERDATA = 32;
    private static final int OFFSET_SCORELIST = 200;
    private static final int LEN_SCORELIST = 360;
    private static final int OFFSET_SC_CHARECNT = 560;
    private static final int LEN_SC_CHARECNT = 1800;
    private static final int OFFSET_SC_BONUSCNT = 2360;
    private static final int LEN_SC_BONUSCNT = 1800;

    private static final int OFFSET_STAGE_PROGRESS = 102;
    private static final int OFFSET_MUSIC_PLAYED = 116;
    private static final int LEN_MUSIC_PLAYED = BgmCatalog.TRACK_COUNT;
    public static final int MUSIC_COUNT = BgmCatalog.TRACK_COUNT;

    public static final GameProgress INSTANCE = new GameProgress();

    // userData[] layout (mirrors DoJa  userdata block).
    public static final int UD_TOTAL_BOOT_TIME_SEC = 0;
    public static final int UD_TOTAL_PLAY_TIME_SEC = 1;
    public static final int UD_TOTAL_CLEAR_COUNT = 2;
    public static final int UD_TOTAL_CONTINUE_COUNT = 3;
    public static final int UD_TOTAL_RETRY_COUNT = 4;

    public static final int UD_PRACTICE_EASY_COUNT = 5;
    public static final int UD_PRACTICE_NORMAL_COUNT = 6;
    public static final int UD_PRACTICE_HARD_COUNT = 7;
    public static final int UD_PRACTICE_LUNATIC_COUNT = 8;

    public static final int UD_NORMAL_EASY_COUNT = 9;
    public static final int UD_NORMAL_NORMAL_COUNT = 10;
    public static final int UD_NORMAL_HARD_COUNT = 11;
    public static final int UD_NORMAL_LUNATIC_COUNT = 12;

    public static final int UD_EXTRA_COUNT = 13;

    private final int[][] scBonusCnt;
    private final int[][] scChareCnt;
    private final int[][] scoreList;
    private final int[] userData;
    private final int[] musicPlayed;
    private final int[] stageProgress = new int[4];

    public static final int ALL_UNITS = -1;

    private boolean loaded;

    private GameProgress() {
        scBonusCnt = new int[UNIT_COUNT][SPELL_COUNT];
        scChareCnt = new int[UNIT_COUNT][SPELL_COUNT];
        scoreList = new int[UNIT_COUNT][15];
        userData = new int[14];
        musicPlayed = new int[MUSIC_COUNT];
    }

    public synchronized void initialize() {
        ensureLoaded();
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loadUserData();
        loadScoreList();
        loadSpellCounts();
        loadMusicPlayed();
        byte[] stages = SpStore.read(OFFSET_STAGE_PROGRESS, stageProgress.length);
        for (int i = 0; i < stages.length; i++) {
            stageProgress[i] = stages[i] & 0xFF;
        }
        loaded = true;
    }

    public synchronized int[] getUserData() {
        ensureLoaded();
        int[] copy = new int[userData.length];
        System.arraycopy(userData, 0, copy, 0, copy.length);
        return copy;
    }

    public synchronized int getUserStat(int index) {
        if (index < 0 || index >= userData.length) {
            throw new IllegalArgumentException("Invalid statistic: " + index);
        }
        ensureLoaded();
        return userData[index];
    }

    public synchronized int[] getScoreList(int unit) {
        requireUnit(unit);
        ensureLoaded();
        int[] copy = new int[15];
        System.arraycopy(scoreList[unit], 0, copy, 0, copy.length);
        return copy;
    }

    public synchronized int getHighScore(int unit, int level) {
        requireUnit(unit);
        requireLevel(level);
        ensureLoaded();
        return scoreList[unit][level * 3];
    }

    /** Copies one unit, or the sum of all units, into buffers owned by the caller. */
    public synchronized void copySpellCounts(int unit, int[] bonus, int[] seen) {
        if (unit != ALL_UNITS) {
            requireUnit(unit);
        }
        if (bonus == null || seen == null || bonus.length < SPELL_COUNT || seen.length < SPELL_COUNT) {
            throw new IllegalArgumentException("Invalid spell count buffers");
        }
        ensureLoaded();
        for (int spell = 0; spell < SPELL_COUNT; spell++) {
            int b = 0;
            int s = 0;
            int first = (unit == ALL_UNITS) ? 0 : unit;
            int end = (unit == ALL_UNITS) ? UNIT_COUNT : unit + 1;
            for (int u = first; u < end; u++) {
                b += scBonusCnt[u][spell];
                s += scChareCnt[u][spell];
            }
            bonus[spell] = b;
            seen[spell] = s;
        }
    }

    public synchronized int countCapturedSpells() {
        ensureLoaded();
        int count = 0;
        for (int spell = 0; spell < SPELL_COUNT; spell++) {
            for (int unit = 0; unit < UNIT_COUNT; unit++) {
                if (scBonusCnt[unit][spell] != 0) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    public synchronized int[] getStageProgress() {
        ensureLoaded();
        int[] copy = new int[stageProgress.length];
        System.arraycopy(stageProgress, 0, copy, 0, copy.length);
        return copy;
    }

    public synchronized void recordStageReached(int level, int stage) {
        if (level < 0 || level >= stageProgress.length || stage < 0 || stage > 255) {
            throw new IllegalArgumentException("Invalid stage progress");
        }
        ensureLoaded();
        if (stage > stageProgress[level]) {
            SpStore.write(OFFSET_STAGE_PROGRESS + level, new byte[] {(byte) stage });
            stageProgress[level] = stage;
        }
    }

    public synchronized boolean isMusicPlayed(int track) {
        requireTrack(track);
        ensureLoaded();
        return musicPlayed[track] != 0;
    }

    public synchronized void markMusicPlayed(int track) {
        requireTrack(track);
        ensureLoaded();
        if (musicPlayed[track] == 0) {
            SpStore.write(OFFSET_MUSIC_PLAYED + track, new byte[] { 1 });
            musicPlayed[track] = 1;
        }
    }

    public synchronized void recordScore(int unit, int level, int score) {
        requireUnit(unit);
        requireLevel(level);
        ensureLoaded();
        int[] next = new int[15];
        System.arraycopy(scoreList[unit], 0, next, 0, next.length);
        int base = level * 3;
        if (score > next[base]) {
            next[base + 2] = next[base + 1];
            next[base + 1] = next[base];
            next[base] = score;
        } else if (score > next[base + 1]) {
            next[base + 2] = next[base + 1];
            next[base + 1] = score;
        } else if (score > next[base + 2]) {
            next[base + 2] = score;
        } else {
            return;
        }
        byte[] data = new byte[60];
        for (int i = 0; i < next.length; i++) {
            writeI32LE(data, i * 4, next[i]);
        }
        SpStore.write(OFFSET_SCORELIST + unit * 60, data);
        System.arraycopy(next, 0, scoreList[unit], 0, next.length);
    }

    public synchronized void recordSpellAttempt(int unit, int spell) {
        recordSpellCount(scChareCnt, OFFSET_SC_CHARECNT, unit, spell);
    }

    public synchronized void recordSpellCapture(int unit, int spell) {
        recordSpellCount(scBonusCnt, OFFSET_SC_BONUSCNT, unit, spell);
    }

    private void recordSpellCount(int[][] counts, int offset, int unit, int spell) {
        requireUnit(unit);
        if (spell < 0 || spell >= SPELL_COUNT) {
            throw new IllegalArgumentException("Invalid spell: " + spell);
        }
        ensureLoaded();
        int next = (counts[unit][spell] + 1) & 0xFFFF;
        byte[] data = new byte[2];
        writeU16LE(data, 0, next);
        SpStore.write(offset + unit * 300 + spell * 2, data);
        counts[unit][spell] = next;
    }

    public synchronized void addTotalBootTimeSeconds(int seconds) {
        addTime(UD_TOTAL_BOOT_TIME_SEC, seconds);
    }

    public synchronized void addTotalPlayTimeSeconds(int seconds) {
        addTime(UD_TOTAL_PLAY_TIME_SEC, seconds);
    }

    private void addTime(int index, int seconds) {
        if (seconds <= 0) {
            return;
        }
        ensureLoaded();
        long value = (long) userData[index] + seconds;
        int next = (value > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) value;
        byte[] data = new byte[4];
        writeI32LE(data, 0, next);
        SpStore.write(OFFSET_USERDATA + index * 4, data);
        userData[index] = next;
    }

    public synchronized void incClearCount() {
        incrementCounter(UD_TOTAL_CLEAR_COUNT);
    }

    public synchronized void incContinueCount() {
        incrementCounter(UD_TOTAL_CONTINUE_COUNT);
    }

    public synchronized void incRetryCount() {
        incrementCounter(UD_TOTAL_RETRY_COUNT);
    }

    public synchronized void incPlayCountExtra() {
        incrementCounter(UD_EXTRA_COUNT);
    }

    public synchronized void incPlayCountNormalByDifficulty(int level) {
        if (level < 0 || level > BattleSessionConfig.DIFFICULTY_LUNATIC) {
            throw new IllegalArgumentException("Invalid story difficulty: " + level);
        }
        incrementCounter(UD_NORMAL_EASY_COUNT + level);
    }

    public synchronized void incPlayCountPracticeByDifficulty(int level) {
        if (level < 0 || level > BattleSessionConfig.DIFFICULTY_LUNATIC) {
            throw new IllegalArgumentException("Invalid practice difficulty: " + level);
        }
        incrementCounter(UD_PRACTICE_EASY_COUNT + level);
    }

    private void incrementCounter(int index) {
        ensureLoaded();
        int next = userData[index] + 1;
        if (next > 65535) {
            next = 65535;
        }
        byte[] data = new byte[2];
        writeU16LE(data, 0, next);
        SpStore.write(OFFSET_USERDATA + 8 + (index - 2) * 2, data);
        userData[index] = next;
    }

    private static void requireUnit(int unit) {
        if (unit < 0 || unit >= UNIT_COUNT) {
            throw new IllegalArgumentException("Invalid unit: " + unit);
        }
    }

    private static void requireLevel(int level) {
        if (level < 0 || level > BattleSessionConfig.DIFFICULTY_EXTRA) {
            throw new IllegalArgumentException("Invalid score difficulty: " + level);
        }
    }

    private static void requireTrack(int track) {
        if (track < 0 || track >= MUSIC_COUNT) {
            throw new IllegalArgumentException("Invalid track: " + track);
        }
    }

    private void loadUserData() {
        byte[] data = SpStore.read(OFFSET_USERDATA, LEN_USERDATA);
        for (int i = 0; i < 2; i++) {
            userData[i] = readI32LE(data, i * 4);
        }
        for (int i = 2; i < userData.length; i++) {
            userData[i] = readU16LE(data, 8 + (i - 2) * 2);
        }
    }

    private void loadScoreList() {
        byte[] b = SpStore.read(OFFSET_SCORELIST, LEN_SCORELIST);
        for (int u = 0; u < UNIT_COUNT; u++) {
            for (int i = 0; i < 15; i++) {
                int p = 60 * u + (i << 2);
                scoreList[u][i] = readI32LE(b, p);
            }
        }
    }

    private void loadSpellCounts() {
        byte[] ch = SpStore.read(OFFSET_SC_CHARECNT, LEN_SC_CHARECNT);
        for (int u = 0; u < UNIT_COUNT; u++) {
            for (int i = 0; i < SPELL_COUNT; i++) {
                int p = 300 * u + (i << 1);
                scChareCnt[u][i] = readU16LE(ch, p);
            }
        }

        byte[] bo = SpStore.read(OFFSET_SC_BONUSCNT, LEN_SC_BONUSCNT);
        for (int u = 0; u < UNIT_COUNT; u++) {
            for (int i = 0; i < SPELL_COUNT; i++) {
                int p = 300 * u + (i << 1);
                scBonusCnt[u][i] = readU16LE(bo, p);
            }
        }
    }

    private void loadMusicPlayed() {
        byte[] b = SpStore.read(OFFSET_MUSIC_PLAYED, LEN_MUSIC_PLAYED);
        for (int i = 0; i < MUSIC_COUNT; i++) {
            musicPlayed[i] = b[i] & 0xFF;
        }
    }

    private static int readU16LE(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }

    private static int readI32LE(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    private static void writeU16LE(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xFF);
        b[p + 1] = (byte) ((v >> 8) & 0xFF);
    }

    private static void writeI32LE(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xFF);
        b[p + 1] = (byte) ((v >> 8) & 0xFF);
        b[p + 2] = (byte) ((v >> 16) & 0xFF);
        b[p + 3] = (byte) ((v >> 24) & 0xFF);
    }
}
