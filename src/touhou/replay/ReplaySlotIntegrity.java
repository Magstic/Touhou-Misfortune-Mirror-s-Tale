package touhou.replay;

final class ReplaySlotIntegrity {
    private ReplaySlotIntegrity() {
    }

    static boolean seal(ReplaySlotData data) {
        if (!hasValidPayloadShape(data)) {
            return false;
        }
        data.header.setPayloadIntegrity(compute(data));
        return true;
    }

    static boolean isValid(ReplaySlotData data) {
        return hasValidPayloadShape(data) && data.header.hasCurrentFormat()
                && data.header.getPayloadChecksum() == compute(data);
    }

    private static boolean hasValidPayloadShape(ReplaySlotData data) {
        if (data == null || data.header == null || data.name == null || data.fullInput == null) {
            return false;
        }
        int headerFlag = data.header.getFlag();
        if (headerFlag != ReplayHeader.FLAG_LOCAL && headerFlag != ReplayHeader.FLAG_IMPORTED) {
            return false;
        }
        if (data.header.getDialoguePageCountLen() > ReplayHeader.DIALOGUE_MAX) {
            return false;
        }
        int bossStartFrame = data.header.getBossStartFrame();
        if (bossStartFrame < -1 || bossStartFrame >= ReplayCodec.FRAME_COUNT) {
            return false;
        }
        if (data.name.length != ReplayFormat.NAME_SIZE || data.fullInput.length != ReplayCodec.ENCODED_SIZE) {
            return false;
        }
        if (data.stageSnapshot == null || data.stageSnapshot.length == 0
                || data.stageSnapshot.length > ReplayFormat.SNAPSHOT_MAX_SIZE) {
            return false;
        }
        return ReplayCodec.decode(data.fullInput) != null;
    }

    private static int compute(ReplaySlotData data) {
        int checksum = ReplayChecksum.INITIAL;
        byte[] headerBytes = data.header.bytes();
        checksum = ReplayChecksum.update(checksum, headerBytes, 0, ReplayHeader.INTEGRITY_OFFSET);
        checksum = ReplayChecksum.update(checksum, headerBytes,
                ReplayHeader.INTEGRITY_OFFSET + ReplayHeader.INTEGRITY_SIZE,
                ReplayHeader.SIZE - ReplayHeader.INTEGRITY_OFFSET - ReplayHeader.INTEGRITY_SIZE);
        checksum = updateArray(checksum, data.name);
        checksum = updateArray(checksum, data.fullInput);
        return updateArray(checksum, data.stageSnapshot);
    }

    private static int updateArray(int checksum, byte[] data) {
        int out = ReplayChecksum.updateInt(checksum, data.length);
        return ReplayChecksum.update(out, data, 0, data.length);
    }
}
