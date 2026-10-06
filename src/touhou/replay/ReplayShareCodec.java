package touhou.replay;

public final class ReplayShareCodec {
    public static final int MAGIC = 0x50524854; // 'THRP' little-endian
    public static final int VERSION = 3;

    private static final int FIXED_SIZE = 4 + 2 + 2 + 6 * 4 + 4;
    private static final int CHECKSUM_OFFSET = FIXED_SIZE - 4;

    private ReplayShareCodec() {
    }

    public static byte[] encode(ReplaySlotData data) {
        if (!ReplaySlotIntegrity.isValid(data)) {
            return null;
        }

        int flags = data.header.getPayloadFlags();
        int bossLength = (data.bossInput != null) ? data.bossInput.length : 0;
        int stageLength = (data.stageSnapshot != null) ? data.stageSnapshot.length : 0;
        int bossSnapshotLength = (data.bossSnapshot != null) ? data.bossSnapshot.length : 0;
        int total = FIXED_SIZE + ReplayHeader.SIZE + ReplayFormat.NAME_SIZE + ReplayCodec.ENCODED_SIZE
                + bossLength + stageLength + bossSnapshotLength;
        byte[] output = new byte[total];

        int position = 0;
        writeI32LE(output, position, MAGIC);
        position += 4;
        writeI16LE(output, position, VERSION);
        position += 2;
        writeI16LE(output, position, flags);
        position += 2;
        writeI32LE(output, position, ReplayHeader.SIZE);
        position += 4;
        writeI32LE(output, position, ReplayFormat.NAME_SIZE);
        position += 4;
        writeI32LE(output, position, ReplayCodec.ENCODED_SIZE);
        position += 4;
        writeI32LE(output, position, bossLength);
        position += 4;
        writeI32LE(output, position, stageLength);
        position += 4;
        writeI32LE(output, position, bossSnapshotLength);
        position += 4;
        position += 4;

        System.arraycopy(data.header.bytes(), 0, output, position, ReplayHeader.SIZE);
        position += ReplayHeader.SIZE;
        System.arraycopy(data.name, 0, output, position, ReplayFormat.NAME_SIZE);
        position += ReplayFormat.NAME_SIZE;
        System.arraycopy(data.fullInput, 0, output, position, ReplayCodec.ENCODED_SIZE);
        position += ReplayCodec.ENCODED_SIZE;
        if (bossLength > 0) {
            System.arraycopy(data.bossInput, 0, output, position, bossLength);
            position += bossLength;
        }
        if (stageLength > 0) {
            System.arraycopy(data.stageSnapshot, 0, output, position, stageLength);
            position += stageLength;
        }
        if (bossSnapshotLength > 0) {
            System.arraycopy(data.bossSnapshot, 0, output, position, bossSnapshotLength);
        }

        writeI32LE(output, CHECKSUM_OFFSET, packetChecksum(output));
        return output;
    }

    public static ReplaySlotData decode(byte[] packet) {
        if (packet == null || packet.length < FIXED_SIZE) {
            return null;
        }
        if (readI32LE(packet, 0) != MAGIC || readU16LE(packet, 4) != VERSION) {
            return null;
        }

        int flags = readU16LE(packet, 6);
        if ((flags & ~ReplaySlotIntegrity.FLAGS_MASK) != 0) {
            return null;
        }
        int headerLength = readI32LE(packet, 8);
        int nameLength = readI32LE(packet, 12);
        int fullLength = readI32LE(packet, 16);
        int bossLength = readI32LE(packet, 20);
        int stageLength = readI32LE(packet, 24);
        int bossSnapshotLength = readI32LE(packet, 28);

        if (headerLength != ReplayHeader.SIZE || nameLength != ReplayFormat.NAME_SIZE
                || fullLength != ReplayCodec.ENCODED_SIZE
                || (bossLength != 0 && bossLength != ReplayCodec.ENCODED_SIZE)
                || stageLength <= 0 || stageLength > ReplayFormat.SNAPSHOT_MAX_SIZE
                || bossSnapshotLength < 0 || bossSnapshotLength > ReplayFormat.SNAPSHOT_MAX_SIZE) {
            return null;
        }
        if (((flags & ReplaySlotIntegrity.FLAG_HAS_BOSS_INPUT) != 0) != (bossLength > 0)
                || ((flags & ReplaySlotIntegrity.FLAG_HAS_BOSS_SNAPSHOT) != 0) != (bossSnapshotLength > 0)) {
            return null;
        }

        long expectedLength = (long) FIXED_SIZE + headerLength + nameLength + fullLength
                + bossLength + stageLength + bossSnapshotLength;
        if (expectedLength != packet.length
                || readI32LE(packet, CHECKSUM_OFFSET) != packetChecksum(packet)) {
            return null;
        }

        int position = FIXED_SIZE;
        ReplayHeader header = new ReplayHeader();
        header.load(copy(packet, position, headerLength));
        position += headerLength;
        byte[] name = copy(packet, position, nameLength);
        position += nameLength;
        byte[] fullInput = copy(packet, position, fullLength);
        position += fullLength;
        byte[] bossInput = null;
        if (bossLength > 0) {
            bossInput = copy(packet, position, bossLength);
            position += bossLength;
        }
        byte[] stageSnapshot = null;
        if (stageLength > 0) {
            stageSnapshot = copy(packet, position, stageLength);
            position += stageLength;
        }
        byte[] bossSnapshot = null;
        if (bossSnapshotLength > 0) {
            bossSnapshot = copy(packet, position, bossSnapshotLength);
        }

        ReplaySlotData data = new ReplaySlotData(header, name, fullInput, bossInput, stageSnapshot, bossSnapshot);
        return ReplaySlotIntegrity.isValid(data) ? data : null;
    }

    private static int packetChecksum(byte[] packet) {
        int checksum = ReplayChecksum.update(ReplayChecksum.INITIAL, packet, 0, CHECKSUM_OFFSET);
        return ReplayChecksum.update(checksum, packet, CHECKSUM_OFFSET + 4,
                packet.length - CHECKSUM_OFFSET - 4);
    }

    private static byte[] copy(byte[] source, int offset, int length) {
        byte[] output = new byte[length];
        System.arraycopy(source, offset, output, 0, length);
        return output;
    }

    private static int readU16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static int readI32LE(byte[] data, int offset) {
        return ReplayChecksum.readI32LE(data, offset);
    }

    private static void writeI16LE(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void writeI32LE(byte[] data, int offset, int value) {
        ReplayChecksum.writeI32LE(data, offset, value);
    }
}
