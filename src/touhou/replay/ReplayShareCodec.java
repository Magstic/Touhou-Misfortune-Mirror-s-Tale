package touhou.replay;

public final class ReplayShareCodec {
    public static final int MAGIC = 0x50524854; // 'THRP' little-endian
    public static final int VERSION = 4;

    private static final int FIXED_SIZE = 4 + 2 + 4 * 4 + 4;
    private static final int CHECKSUM_OFFSET = FIXED_SIZE - 4;

    private ReplayShareCodec() {
    }

    public static byte[] encode(ReplaySlotData data) {
        if (!ReplaySlotIntegrity.isValid(data)) {
            return null;
        }

        int stageLength = data.stageSnapshot.length;
        int total = FIXED_SIZE + ReplayHeader.SIZE + ReplayFormat.NAME_SIZE + ReplayCodec.ENCODED_SIZE + stageLength;
        byte[] output = new byte[total];

        int position = 0;
        writeI32LE(output, position, MAGIC);
        position += 4;
        writeI16LE(output, position, VERSION);
        position += 2;
        writeI32LE(output, position, ReplayHeader.SIZE);
        position += 4;
        writeI32LE(output, position, ReplayFormat.NAME_SIZE);
        position += 4;
        writeI32LE(output, position, ReplayCodec.ENCODED_SIZE);
        position += 4;
        writeI32LE(output, position, stageLength);
        position += 4;
        position += 4;

        System.arraycopy(data.header.bytes(), 0, output, position, ReplayHeader.SIZE);
        position += ReplayHeader.SIZE;
        System.arraycopy(data.name, 0, output, position, ReplayFormat.NAME_SIZE);
        position += ReplayFormat.NAME_SIZE;
        System.arraycopy(data.fullInput, 0, output, position, ReplayCodec.ENCODED_SIZE);
        position += ReplayCodec.ENCODED_SIZE;
        System.arraycopy(data.stageSnapshot, 0, output, position, stageLength);

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

        int headerLength = readI32LE(packet, 6);
        int nameLength = readI32LE(packet, 10);
        int fullLength = readI32LE(packet, 14);
        int stageLength = readI32LE(packet, 18);
        if (headerLength != ReplayHeader.SIZE || nameLength != ReplayFormat.NAME_SIZE
                || fullLength != ReplayCodec.ENCODED_SIZE
                || stageLength <= 0 || stageLength > ReplayFormat.SNAPSHOT_MAX_SIZE) {
            return null;
        }

        long expectedLength = (long) FIXED_SIZE + headerLength + nameLength + fullLength
                + stageLength;
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
        byte[] stageSnapshot = copy(packet, position, stageLength);
        ReplaySlotData data = new ReplaySlotData(header, name, fullInput, stageSnapshot);
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
