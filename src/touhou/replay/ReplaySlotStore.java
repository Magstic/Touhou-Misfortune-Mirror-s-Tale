package touhou.replay;

public final class ReplaySlotStore {
    private ReplaySlotStore() {
    }

    public static boolean warmup() {
        return ReplayRmsStore.warmup() && ReplaySnapshotStore.warmup();
    }

    public static boolean saveSlot(int slot, ReplaySlotData data) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT || !ReplaySlotIntegrity.seal(data)) {
            return false;
        }
        if (!ReplayRmsStore.invalidateSlot(slot) || !ReplaySnapshotStore.saveSlot(slot, data.stageSnapshot)) {
            return false;
        }
        return ReplayRmsStore.saveSlot(slot, data.header, data.name, data.fullInput);
    }

    public static ReplaySlotData loadSlot(int slot) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT) {
            return null;
        }
        ReplayHeader header = new ReplayHeader();
        byte[] name = new byte[ReplayFormat.NAME_SIZE];
        byte[] fullInput = new byte[ReplayCodec.ENCODED_SIZE];
        if (!ReplayRmsStore.loadSlot(slot, header, name, fullInput)) {
            return null;
        }
        byte[] stageSnapshot = ReplaySnapshotStore.loadSlot(slot);
        if (stageSnapshot == null) {
            return null;
        }
        ReplaySlotData data = new ReplaySlotData(header, name, fullInput, stageSnapshot);
        return ReplaySlotIntegrity.isValid(data) ? data : null;
    }

    public static boolean clearSlot(int slot) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT || !ReplayRmsStore.invalidateSlot(slot)) {
            return false;
        }
        boolean stageCleared = ReplaySnapshotStore.clearSlot(slot);
        boolean replayCleared = ReplayRmsStore.clearSlot(slot);
        return stageCleared && replayCleared;
    }

    public static boolean loadAllSlotSummaries(ReplayHeader[] headers, byte[][] names, boolean[] hasStageSnapshot) {
        if (!ReplayRmsStore.loadAllSlotHeadersAndNames(headers, names)) {
            clearPresence(hasStageSnapshot);
            return false;
        }
        if (!ReplaySnapshotStore.loadPresence(hasStageSnapshot)) {
            return false;
        }
        for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
            ReplayHeader header = headers[slot];
            hasStageSnapshot[slot] = hasStageSnapshot[slot] && header != null && header.hasCurrentFormat();
        }
        return true;
    }

    public static boolean loadSlotHeaderAndName(int slot, ReplayHeader header, byte[] name) {
        return ReplayRmsStore.loadSlotHeaderAndName(slot, header, name);
    }

    private static void clearPresence(boolean[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = false;
        }
    }
}
