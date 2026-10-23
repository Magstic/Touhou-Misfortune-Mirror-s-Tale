package touhou;

public final class BgmCatalog {
    public static final int TRACK_COUNT = 18;
    public static final int FIRST_STAGE_TRACK = 1;
    public static final int FIRST_BOSS_TRACK = 2;
    public static final int LAST_STAGE_TRACK = 14;
    public static final int LAST_WORD = 17;
    private static final int STAGE_6_TRACK = 11;
    private static final int ENDING_TRACK = 15;
    private static final int STAFF_ROLL_TRACK = 16;

    private BgmCatalog() {
    }

    public static boolean isTrack(int trackId) {
        return trackId >= 0 && trackId < TRACK_COUNT;
    }

    public static boolean isBattleTrack(int trackId) {
        return (trackId >= FIRST_STAGE_TRACK && trackId <= LAST_STAGE_TRACK) || trackId == LAST_WORD;
    }

    public static boolean isSplitTrack(int trackId) {
        return trackId != STAGE_6_TRACK && trackId != ENDING_TRACK && trackId != STAFF_ROLL_TRACK;
    }
}
