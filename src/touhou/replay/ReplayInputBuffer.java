package touhou.replay;

public final class ReplayInputBuffer {
    public static final int FRAME_COUNT = ReplayCodec.FRAME_COUNT;

    private final byte[] frames = new byte[FRAME_COUNT];
    private int pos;

    public void clear() {
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = 0;
        }
        pos = 0;
    }

    public int getPos() {
        return pos;
    }

    public void putNext(byte v) {
        if (pos < 0 || pos >= FRAME_COUNT) {
            pos++;
            return;
        }
        frames[pos++] = v;
    }

    public byte[] raw() {
        return frames;
    }
}
