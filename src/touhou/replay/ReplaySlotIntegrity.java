package touhou.replay;

final class ReplaySlotIntegrity {
    static final int FLAG_HAS_BOSS_INPUT = 0x01;
    static final int FLAG_HAS_BOSS_SNAPSHOT = 0x02;
    static final int FLAGS_MASK = FLAG_HAS_BOSS_INPUT | FLAG_HAS_BOSS_SNAPSHOT;

    private ReplaySlotIntegrity() {
    }

    static boolean seal(ReplaySlotData data) {
        if (!hasValidPayloadShape(data)) {
            return false;
        }
        int flags = payloadFlags(data);
        data.header.setPayloadIntegrity(flags, compute(data));
        return true;
    }

    static boolean isValid(ReplaySlotData data) {
        if (!hasValidPayloadShape(data) || !data.header.hasCurrentFormat()) {
            return false;
        }
        int flags = payloadFlags(data);
        return data.header.getPayloadFlags() == flags
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
        if (bossStartFrame < -1 || bossStartFrame > ReplayCodec.FRAME_COUNT) {
            return false;
        }
        if ((bossStartFrame < 0 && (data.bossInput != null || data.bossSnapshot != null))
                || (bossStartFrame >= 0 && data.bossInput == null)) {
            return false;
        }
        if (data.name.length != ReplayFormat.NAME_SIZE || data.fullInput.length != ReplayCodec.ENCODED_SIZE) {
            return false;
        }
        if (data.bossInput != null && data.bossInput.length != ReplayCodec.ENCODED_SIZE) {
            return false;
        }
        if (data.stageSnapshot == null
                || !hasValidSnapshotShape(data.stageSnapshot)
                || !hasValidSnapshotShape(data.bossSnapshot)) {
            return false;
        }
        if (ReplayCodec.decode(data.fullInput) == null) {
            return false;
        }
        return data.bossInput == null || ReplayCodec.decode(data.bossInput) != null;
    }

    private static boolean hasValidSnapshotShape(byte[] snapshot) {
        return snapshot == null || (snapshot.length > 0 && snapshot.length <= ReplayFormat.SNAPSHOT_MAX_SIZE);
    }

    private static int payloadFlags(ReplaySlotData data) {
        int flags = 0;
        if (data.bossInput != null) {
            flags |= FLAG_HAS_BOSS_INPUT;
        }
        if (data.bossSnapshot != null) {
            flags |= FLAG_HAS_BOSS_SNAPSHOT;
        }
        return flags;
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
        checksum = updateArray(checksum, data.bossInput);
        checksum = updateArray(checksum, data.stageSnapshot);
        return updateArray(checksum, data.bossSnapshot);
    }

    private static int updateArray(int checksum, byte[] data) {
        if (data == null) {
            return ReplayChecksum.updateInt(checksum, -1);
        }
        int out = ReplayChecksum.updateInt(checksum, data.length);
        return ReplayChecksum.update(out, data, 0, data.length);
    }
}
