package touhou;

public final class MusicRoomSettings {
    private static final int OFFSET_MR_SETTINGS = 136;
    private static final int LEN_MR_SETTINGS = 4;

    private static final int MAGIC0 = 0x4D; // 'M'
    private static final int MAGIC1 = 0x52; // 'R'
    private static final int VERSION = 0x01;

    private MusicRoomSettings() {
    }

    public static int loadVolume(int defaultVol) {
        byte[] data = SpStore.read(OFFSET_MR_SETTINGS, LEN_MR_SETTINGS);
        boolean empty = true;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                empty = false;
            }
        }
        if (empty) {
            return clamp100(defaultVol);
        }
        if ((data[1] & 0xFF) != MAGIC0 || (data[2] & 0xFF) != MAGIC1
                || (data[3] & 0xFF) != VERSION || (data[0] & 0xFF) > 100) {
            throw new StorageException("Invalid stored music room settings");
        }
        return data[0] & 0xFF;
    }

    public static void saveVolume(int volume) {
        byte[] data = new byte[LEN_MR_SETTINGS];
        data[0] = (byte) clamp100(volume);
        data[1] = (byte) MAGIC0;
        data[2] = (byte) MAGIC1;
        data[3] = (byte) VERSION;
        SpStore.write(OFFSET_MR_SETTINGS, data);
    }

    private static int clamp100(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 100) {
            return 100;
        }
        return v;
    }
}
