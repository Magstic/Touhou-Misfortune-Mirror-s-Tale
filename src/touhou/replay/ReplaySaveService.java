package touhou.replay;

public final class ReplaySaveService {
    private static ReplaySlotData pending;

    private ReplaySaveService() {
    }

    public static boolean hasPending() {
        return pending != null;
    }

    public static void clearPending() {
        pending = null;
    }

    public static boolean armPending(ReplayHeader header, byte[] name, ReplayStageRecorder recorder,
            byte[] bossSnapshot, byte[] stageSnapshot) {
        if (header == null || recorder == null) {
            return false;
        }

        byte[] fullInput = recorder.encodeFull();
        if (fullInput == null) {
            return false;
        }
        byte[] bossInput = recorder.encodeBossOnly();
        if (recorder.getBossStartFrame() >= 0 && bossInput == null) {
            return false;
        }

        byte[] fixedName = new byte[ReplayFormat.NAME_SIZE];
        if (name != null) {
            int length = name.length;
            if (length > fixedName.length) {
                length = fixedName.length;
            }
            System.arraycopy(name, 0, fixedName, 0, length);
        }

        ReplaySlotData data = new ReplaySlotData(header, fixedName, fullInput, bossInput, stageSnapshot, bossSnapshot);
        if (!ReplaySlotIntegrity.seal(data)) {
            return false;
        }
        pending = ReplaySlotData.copyOf(data);
        return pending != null;
    }

    public static boolean savePendingToSlot(int slot) {
        if (pending == null) {
            return false;
        }
        ReplaySlotData data = ReplaySlotData.copyOf(pending);
        return data != null && ReplaySlotStore.saveSlot(slot, data);
    }
}
