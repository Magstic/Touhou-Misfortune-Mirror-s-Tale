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

    public static boolean armPending(ReplayHeader header, ReplayStageRecorder recorder, byte[] stageSnapshot) {
        byte[] fullInput = recorder.encodeFull();
        if (fullInput == null) {
            return false;
        }
        byte[] fixedName = new byte[ReplayFormat.NAME_SIZE];

        ReplaySlotData data = new ReplaySlotData(header, fixedName, fullInput, stageSnapshot);
        if (!ReplaySlotIntegrity.seal(data)) {
            return false;
        }
        pending = ReplaySlotData.copyOf(data);
        return true;
    }

    public static boolean savePendingToSlot(int slot) {
        if (pending == null) {
            return false;
        }
        ReplaySlotData data = ReplaySlotData.copyOf(pending);
        return ReplaySlotStore.saveSlot(slot, data);
    }
}
