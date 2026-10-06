package touhou.replay;

public final class ReplaySlotStore {
    private static final ReplaySnapshotStore STAGE_SNAPSHOTS = new ReplaySnapshotStore("th_replay_stage");
    private static final ReplaySnapshotStore BOSS_SNAPSHOTS = new ReplaySnapshotStore("th_replay_boss");

    private ReplaySlotStore() {
    }

    public static boolean warmup() {
        return ReplayRmsStore.warmup()
                && STAGE_SNAPSHOTS.warmup()
                && BOSS_SNAPSHOTS.warmup();
    }

    public static boolean saveSlot(int slot, ReplaySlotData data) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT || !ReplaySlotIntegrity.seal(data)) {
            return false;
        }

        if (!ReplayRmsStore.invalidateSlot(slot)) {
            return false;
        }
        if (!STAGE_SNAPSHOTS.saveSlot(slot, data.stageSnapshot)) {
            return false;
        }
        if (!BOSS_SNAPSHOTS.saveSlot(slot, data.bossSnapshot)) {
            return false;
        }
        return ReplayRmsStore.saveSlot(slot, data.header, data.name, data.fullInput, data.bossInput);
    }

    public static ReplaySlotData loadSlot(int slot) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT) {
            return null;
        }

        ReplayHeader header = new ReplayHeader();
        byte[] name = new byte[ReplayFormat.NAME_SIZE];
        byte[] fullInput = new byte[ReplayCodec.ENCODED_SIZE];
        byte[] storedBossInput = new byte[ReplayCodec.ENCODED_SIZE];
        if (!ReplayRmsStore.loadSlot(slot, header, name, fullInput, storedBossInput)) {
            return null;
        }

        int flags = header.getPayloadFlags();
        byte[] bossInput = ((flags & ReplaySlotIntegrity.FLAG_HAS_BOSS_INPUT) != 0) ? storedBossInput : null;
        byte[] stageSnapshot = STAGE_SNAPSHOTS.loadSlot(slot);
        if (stageSnapshot == null) {
            return null;
        }
        byte[] bossSnapshot = null;
        if ((flags & ReplaySlotIntegrity.FLAG_HAS_BOSS_SNAPSHOT) != 0) {
            bossSnapshot = BOSS_SNAPSHOTS.loadSlot(slot);
            if (bossSnapshot == null) {
                return null;
            }
        }

        ReplaySlotData data = new ReplaySlotData(header, name, fullInput, bossInput, stageSnapshot, bossSnapshot);
        return ReplaySlotIntegrity.isValid(data) ? data : null;
    }

    public static boolean clearSlot(int slot) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT || !ReplayRmsStore.invalidateSlot(slot)) {
            return false;
        }
        boolean stageCleared = STAGE_SNAPSHOTS.clearSlot(slot);
        boolean bossCleared = BOSS_SNAPSHOTS.clearSlot(slot);
        boolean replayCleared = ReplayRmsStore.clearSlot(slot);
        return stageCleared && bossCleared && replayCleared;
    }

    public static boolean loadAllSlotSummaries(ReplayHeader[] headers, byte[][] names,
            boolean[] hasStageSnapshot, boolean[] hasBossSnapshot) {
        if (!ReplayRmsStore.loadAllSlotHeadersAndNames(headers, names)) {
            clearPresence(hasStageSnapshot);
            clearPresence(hasBossSnapshot);
            return false;
        }
        boolean stageLoaded = STAGE_SNAPSHOTS.loadPresence(hasStageSnapshot);
        boolean bossLoaded = BOSS_SNAPSHOTS.loadPresence(hasBossSnapshot);
        if (!stageLoaded || !bossLoaded) {
            return false;
        }
        for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
            ReplayHeader header = headers[slot];
            int flags = (header != null && header.hasCurrentFormat()) ? header.getPayloadFlags() : 0;
            hasStageSnapshot[slot] = hasStageSnapshot[slot] && header != null && header.hasCurrentFormat();
            hasBossSnapshot[slot] = hasBossSnapshot[slot]
                    && (flags & ReplaySlotIntegrity.FLAG_HAS_BOSS_SNAPSHOT) != 0;
        }
        return true;
    }

    public static boolean loadSlotHeaderAndName(int slot, ReplayHeader header, byte[] name) {
        return ReplayRmsStore.loadSlotHeaderAndName(slot, header, name);
    }

    public static byte[] loadStageSnapshot(int slot) {
        ReplaySlotData data = loadSlot(slot);
        return (data != null) ? data.stageSnapshot : null;
    }

    public static byte[] loadBossSnapshot(int slot) {
        ReplaySlotData data = loadSlot(slot);
        return (data != null) ? data.bossSnapshot : null;
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
