package touhou.replay;

public final class ReplaySnapshotReader {
    private final byte[] data;
    private final int limit;
    private int pos;
    private boolean valid;

    public ReplaySnapshotReader(byte[] data, int length) {
        this.data = (data != null) ? data : new byte[0];
        valid = data != null && length >= 0 && length <= this.data.length;
        limit = valid ? length : 0;
        this.pos = 0;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isAtEnd() {
        return valid && pos == limit;
    }

    public int readU8() {
        if (!valid || pos >= limit) {
            valid = false;
            return 0;
        }
        return data[pos++] & 0xFF;
    }

    public boolean readBool() {
        int value = readU8();
        if (value > 1) {
            valid = false;
        }
        return value != 0;
    }

    public int readI32LE() {
        int b0 = readU8();
        int b1 = readU8();
        int b2 = readU8();
        int b3 = readU8();
        return (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
    }

}
