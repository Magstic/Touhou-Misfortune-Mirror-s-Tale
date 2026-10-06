package touhou.replay;

public final class ReplayCodec {
    public static final int FRAME_COUNT = 30720;
    public static final int ENCODED_SIZE = 7168;

    private ReplayCodec() {
    }

    // Fixed-size RLE: (len,value) pairs with len in 1..127.
    public static byte[] encode(byte[] frames) {
        if (frames == null || frames.length != FRAME_COUNT) {
            return null;
        }

        byte[] out = new byte[ENCODED_SIZE];
        int outPos = 0;
        int i = 0;
        while (i < FRAME_COUNT) {
            byte v = frames[i];
            int run = 1;
            while ((i + run) < FRAME_COUNT && run < 127 && frames[i + run] == v) {
                run++;
            }
            if ((outPos + 1) >= ENCODED_SIZE) {
                return null;
            }
            out[outPos++] = (byte) (run & 0xFF);
            out[outPos++] = v;
            i += run;
        }
        return out;
    }

    public static byte[] decode(byte[] encoded) {
        if (encoded == null || encoded.length != ENCODED_SIZE) {
            return null;
        }

        byte[] out = new byte[FRAME_COUNT];
        int outPos = 0;
        int inPos = 0;
        while (outPos < FRAME_COUNT) {
            if ((inPos + 1) >= ENCODED_SIZE) {
                return null;
            }
            int run = encoded[inPos] & 0xFF;
            if (run <= 0 || run > 127 || outPos + run > FRAME_COUNT) {
                return null;
            }
            byte v = encoded[inPos + 1];
            int end = outPos + run;
            while (outPos < end) {
                out[outPos++] = v;
            }
            inPos += 2;
        }

        while (inPos < ENCODED_SIZE) {
            if (encoded[inPos++] != 0) {
                return null;
            }
        }
        return out;
    }
}
