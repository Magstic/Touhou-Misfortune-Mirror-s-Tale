package touhou.replay;

public final class ReplaySlotData {
    public final ReplayHeader header;
    public final byte[] name;
    public final byte[] fullInput;
    public final byte[] bossInput;
    public final byte[] stageSnapshot;
    public final byte[] bossSnapshot;

    public ReplaySlotData(ReplayHeader header, byte[] name, byte[] fullInput, byte[] bossInput, byte[] stageSnapshot, byte[] bossSnapshot) {
        this.header = header;
        this.name = name;
        this.fullInput = fullInput;
        this.bossInput = bossInput;
        this.stageSnapshot = stageSnapshot;
        this.bossSnapshot = bossSnapshot;
    }

    public static ReplaySlotData copyOf(ReplaySlotData source) {
        if (source == null) {
            return null;
        }
        ReplayHeader headerCopy = null;
        if (source.header != null) {
            headerCopy = new ReplayHeader();
            headerCopy.load(source.header.bytes());
        }
        return new ReplaySlotData(
                headerCopy,
                copy(source.name),
                copy(source.fullInput),
                copy(source.bossInput),
                copy(source.stageSnapshot),
                copy(source.bossSnapshot));
    }

    private static byte[] copy(byte[] source) {
        if (source == null) {
            return null;
        }
        byte[] out = new byte[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }
}
