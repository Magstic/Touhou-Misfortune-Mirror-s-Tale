package touhou.replay;

public final class ReplayShareService {
    private ReplayShareService() {
    }

    public static byte[] exportSlotPacket(int slot) {
        ReplaySlotData data = ReplaySlotStore.loadSlot(slot);
        return (data != null && data.header.getFlag() == ReplayHeader.FLAG_LOCAL)
                ? ReplayShareCodec.encode(data) : null;
    }

    public static boolean importToSlotFromPacket(int slot, byte[] packet) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT) {
            return false;
        }
        ReplaySlotData data = ReplayShareCodec.decode(packet);
        if (data == null) {
            return false;
        }
        return importToSlot(slot, data);
    }

    public static boolean importToSlot(int slot, ReplaySlotData data) {
        data.header.setFlag(ReplayHeader.FLAG_IMPORTED);
        return ReplaySlotStore.saveSlot(slot, data);
    }
}
