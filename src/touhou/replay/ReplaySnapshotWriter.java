package touhou.replay;

import java.io.ByteArrayOutputStream;

public final class ReplaySnapshotWriter {
    private final ByteArrayOutputStream out;

    public ReplaySnapshotWriter(int initialCapacity) {
        out = new ByteArrayOutputStream(initialCapacity);
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }

    public void writeBool(boolean v) {
        out.write(v ? 1 : 0);
    }

    public void writeI32LE(int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }
}
