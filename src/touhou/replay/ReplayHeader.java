package touhou.replay;

public final class ReplayHeader {
    public static final int SIZE = 64;
    static final int INTEGRITY_OFFSET = 8;
    static final int INTEGRITY_SIZE = 16;

    private static final int OFFSET_FLAG = 0;
    private static final int OFFSET_DIFFICULTY = 1;
    private static final int OFFSET_CHARACTER = 2;
    private static final int OFFSET_STAGE = 3;
    private static final int OFFSET_SCORE = 4;
    private static final int OFFSET_VERSION = INTEGRITY_OFFSET + 4;
    private static final int OFFSET_RESERVED = INTEGRITY_OFFSET + 5;
    private static final int OFFSET_CHECKSUM = INTEGRITY_OFFSET + 6;
    private static final int OFFSET_SHOT_TYPE = 24;
    private static final int OFFSET_MODE = 25;
    private static final int OFFSET_SPELL = 26;
    private static final int OFFSET_BOSS_FRAME = 28;
    private static final int OFFSET_DIALOGUE_COUNT = 32;
    private static final int OFFSET_DIALOGUE_PAGES = 33;

    private static final int FORMAT_MAGIC = 0x34504852; // 'RHP4'
    private static final int FORMAT_VERSION = 1;

    public static final int DIALOGUE_MAX = 31;

    public static final int FLAG_NONE = 0;
    public static final int FLAG_LOCAL = 1;
    public static final int FLAG_IMPORTED = 2;

    private final byte[] data = new byte[SIZE];

    public ReplayHeader() {
    }

    public byte[] bytes() {
        return data;
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            data[i] = 0;
        }
    }

    public void load(byte[] src) {
        if (src == null) {
            return;
        }
        int n = src.length;
        if (n > SIZE) {
            n = SIZE;
        }
        for (int i = 0; i < n; i++) {
            data[i] = src[i];
        }
        for (int i = n; i < SIZE; i++) {
            data[i] = 0;
        }
    }

    public int getFlag() {
        return data[OFFSET_FLAG] & 0xFF;
    }

    public void setFlag(int v) {
        data[OFFSET_FLAG] = (byte) (v & 0xFF);
    }

    public int getDifficulty() {
        return data[OFFSET_DIFFICULTY] & 0xFF;
    }

    public void setDifficulty(int v) {
        data[OFFSET_DIFFICULTY] = (byte) (v & 0xFF);
    }

    public int getChara() {
        return data[OFFSET_CHARACTER] & 0xFF;
    }

    public void setChara(int v) {
        data[OFFSET_CHARACTER] = (byte) (v & 0xFF);
    }

    public int getStage() {
        return data[OFFSET_STAGE] & 0xFF;
    }

    public void setStage(int v) {
        data[OFFSET_STAGE] = (byte) (v & 0xFF);
    }

    public int getScore() {
        return readI32LE(data, OFFSET_SCORE);
    }

    public void setScore(int v) {
        writeI32LE(data, OFFSET_SCORE, v);
    }

    boolean hasCurrentFormat() {
        return readI32LE(data, INTEGRITY_OFFSET) == FORMAT_MAGIC
                && (data[OFFSET_VERSION] & 0xFF) == FORMAT_VERSION
                && data[OFFSET_RESERVED] == 0;
    }

    int getPayloadChecksum() {
        return readI32LE(data, OFFSET_CHECKSUM);
    }

    void setPayloadIntegrity(int checksum) {
        for (int i = 0; i < INTEGRITY_SIZE; i++) {
            data[INTEGRITY_OFFSET + i] = 0;
        }
        writeI32LE(data, INTEGRITY_OFFSET, FORMAT_MAGIC);
        data[OFFSET_VERSION] = (byte) FORMAT_VERSION;
        writeI32LE(data, OFFSET_CHECKSUM, checksum);
    }

    public int getType() {
        return data[OFFSET_SHOT_TYPE] & 0xFF;
    }

    public void setType(int v) {
        data[OFFSET_SHOT_TYPE] = (byte) (v & 0xFF);
    }

    public int getMode() {
        return data[OFFSET_MODE] & 0xFF;
    }

    public void setMode(int v) {
        data[OFFSET_MODE] = (byte) (v & 0xFF);
    }

    public int getSpellId() {
        return data[OFFSET_SPELL] & 0xFF;
    }

    public void setSpellId(int v) {
        data[OFFSET_SPELL] = (byte) (v & 0xFF);
    }

    public int getBossStartFrame() {
        return readI32LE(data, OFFSET_BOSS_FRAME);
    }

    public void setBossStartFrame(int v) {
        writeI32LE(data, OFFSET_BOSS_FRAME, v);
    }

    public int getDialoguePageCountLen() {
        return data[OFFSET_DIALOGUE_COUNT] & 0xFF;
    }

    public void setDialoguePageCountLen(int len) {
        if (len < 0) {
            len = 0;
        }
        if (len > DIALOGUE_MAX) {
            len = DIALOGUE_MAX;
        }
        data[OFFSET_DIALOGUE_COUNT] = (byte) (len & 0xFF);
    }

    public int getDialoguePageCountAt(int idx) {
        if (idx < 0 || idx >= DIALOGUE_MAX) {
            return -1;
        }
        int len = getDialoguePageCountLen();
        if (idx >= len) {
            return -1;
        }
        return data[OFFSET_DIALOGUE_PAGES + idx] & 0xFF;
    }

    public void setDialoguePageCounts(byte[] counts, int len) {
        if (counts == null) {
            setDialoguePageCountLen(0);
            return;
        }
        if (len < 0) {
            len = 0;
        }
        if (len > DIALOGUE_MAX) {
            len = DIALOGUE_MAX;
        }
        setDialoguePageCountLen(len);
        for (int i = 0; i < DIALOGUE_MAX; i++) {
            int v = 0;
            if (i < len && i < counts.length) {
                v = counts[i] & 0xFF;
            }
            data[OFFSET_DIALOGUE_PAGES + i] = (byte) (v & 0xFF);
        }
    }

    private static int readI32LE(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    private static void writeI32LE(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xFF);
        b[p + 1] = (byte) ((v >> 8) & 0xFF);
        b[p + 2] = (byte) ((v >> 16) & 0xFF);
        b[p + 3] = (byte) ((v >> 24) & 0xFF);
    }
}
