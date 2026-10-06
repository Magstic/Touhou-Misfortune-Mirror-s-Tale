package touhou;

public final class BgmCatalog {
    public static final int PACK_MAGIC = 0x42474D50;
    public static final int PACK_VERSION = 1;
    public static final int CODEC_MIDI = 0;
    public static final int PART_COUNT = 3;
    public static final int PART_INTRO = 0;
    public static final int PART_LOOP = 1;
    public static final int PART_SINGLE = 2;
    public static final int TRACK_COUNT = 18;

    private BgmCatalog() {
    }

    public static boolean isTrack(int trackId) {
        return trackId >= 0 && trackId < TRACK_COUNT;
    }
}
