package touhou.replay;

public final class ReplayShareService {
    private ReplayShareService() {
    }

    public static byte[] exportSlotPacket(int slot) {
        ReplaySlotData data = ReplaySlotStore.loadSlot(slot);
        return (data != null) ? ReplayShareCodec.encode(data) : null;
    }

    public static boolean importToSlotFromPacket(int slot, byte[] packet) {
        if (slot < 0 || slot >= ReplayFormat.SLOT_COUNT) {
            return false;
        }
        ReplaySlotData data = ReplayShareCodec.decode(packet);
        if (data == null) {
            return false;
        }
        data.header.setFlag(ReplayHeader.FLAG_IMPORTED);
        return ReplaySlotStore.saveSlot(slot, data);
    }
}
