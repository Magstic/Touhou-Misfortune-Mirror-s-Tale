package touhou.replay;

public final class ReplayChecksum {
    public static final int INITIAL = 0x811C9DC5;

    private ReplayChecksum() {
    }

    public static int update(int checksum, byte[] data, int offset, int length) {
        if (data == null || offset < 0 || length < 0 || offset > data.length - length) {
            return checksum;
        }
        int value = checksum;
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            value ^= data[i] & 0xFF;
            value *= 0x01000193;
        }
        return value;
    }

    public static int updateInt(int checksum, int value) {
        int out = checksum;
        for (int shift = 0; shift < 32; shift += 8) {
            out ^= (value >> shift) & 0xFF;
            out *= 0x01000193;
        }
        return out;
    }

    public static int compute(byte[] data, int offset, int length) {
        return update(INITIAL, data, offset, length);
    }

    public static byte[] append(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] out = new byte[data.length + 4];
        System.arraycopy(data, 0, out, 0, data.length);
        writeI32LE(out, data.length, compute(data, 0, data.length));
        return out;
    }

    public static boolean hasValidFooter(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        int payloadLength = data.length - 4;
        return readI32LE(data, payloadLength) == compute(data, 0, payloadLength);
    }

    static int readI32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    static void writeI32LE(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
}
