package touhou.replay;

public final class ReplayHeader {
    public static final int SIZE = 64;
    static final int INTEGRITY_OFFSET = 8;
    static final int INTEGRITY_SIZE = 16;

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

    public void store(byte[] dst) {
        if (dst == null) {
            return;
        }
        int n = dst.length;
        if (n > SIZE) {
            n = SIZE;
        }
        for (int i = 0; i < n; i++) {
            dst[i] = data[i];
        }
    }

    public int getFlag() {
        return data[0] & 0xFF;
    }

    public void setFlag(int v) {
        data[0] = (byte) (v & 0xFF);
    }

    public int getDifficulty() {
        return data[1] & 0xFF;
    }

    public void setDifficulty(int v) {
        data[1] = (byte) (v & 0xFF);
    }

    public int getChara() {
        return data[2] & 0xFF;
    }

    public void setChara(int v) {
        data[2] = (byte) (v & 0xFF);
    }

    public int getStage() {
        return data[3] & 0xFF;
    }

    public void setStage(int v) {
        data[3] = (byte) (v & 0xFF);
    }

    public int getScore() {
        return readI32LE(data, 4);
    }

    public void setScore(int v) {
        writeI32LE(data, 4, v);
    }

    boolean hasCurrentFormat() {
        return readI32LE(data, INTEGRITY_OFFSET) == FORMAT_MAGIC
                && (data[INTEGRITY_OFFSET + 4] & 0xFF) == FORMAT_VERSION
                && data[INTEGRITY_OFFSET + 5] == 0;
    }

    int getPayloadChecksum() {
        return readI32LE(data, INTEGRITY_OFFSET + 6);
    }

    void setPayloadIntegrity(int checksum) {
        for (int i = 0; i < INTEGRITY_SIZE; i++) {
            data[INTEGRITY_OFFSET + i] = 0;
        }
        writeI32LE(data, INTEGRITY_OFFSET, FORMAT_MAGIC);
        data[INTEGRITY_OFFSET + 4] = (byte) FORMAT_VERSION;
        writeI32LE(data, INTEGRITY_OFFSET + 6, checksum);
    }

    public int getType() {
        return data[24] & 0xFF;
    }

    public void setType(int v) {
        data[24] = (byte) (v & 0xFF);
    }

    public int getMode() {
        return data[25] & 0xFF;
    }

    public void setMode(int v) {
        data[25] = (byte) (v & 0xFF);
    }

    public int getSpellId() {
        return data[26] & 0xFF;
    }

    public void setSpellId(int v) {
        data[26] = (byte) (v & 0xFF);
    }

    public int getBossStartFrame() {
        return readI32LE(data, 28);
    }

    public void setBossStartFrame(int v) {
        writeI32LE(data, 28, v);
    }

    public int getDialoguePageCountLen() {
        return data[32] & 0xFF;
    }

    public void setDialoguePageCountLen(int len) {
        if (len < 0) {
            len = 0;
        }
        if (len > DIALOGUE_MAX) {
            len = DIALOGUE_MAX;
        }
        data[32] = (byte) (len & 0xFF);
    }

    public int getDialoguePageCountAt(int idx) {
        if (idx < 0 || idx >= DIALOGUE_MAX) {
            return -1;
        }
        int len = getDialoguePageCountLen();
        if (idx >= len) {
            return -1;
        }
        return data[33 + idx] & 0xFF;
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
            data[33 + i] = (byte) (v & 0xFF);
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
