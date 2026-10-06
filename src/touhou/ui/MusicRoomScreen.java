package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.GameOptions;
import touhou.GameProgress;
import touhou.BgmCatalog;
import touhou.MusicRoomSettings;
import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class MusicRoomScreen {
    public static final class Result {
        public static final int KIND_NONE = 0;
        public static final int KIND_BACK_TO_TITLE = 1;
        public static final int KIND_PLAY_TRACK = 2;
        public static final int KIND_VOLUME_CHANGED = 3;

        public final int kind;
        public final int trackId;

        public Result(int kind) {
            this.kind = kind;
            this.trackId = -1;
        }

        public Result(int kind, int trackId) {
            this.kind = kind;
            this.trackId = trackId;
        }
    }

    private static final int SOUNDTEST_MAX = BgmCatalog.TRACK_COUNT;
    private static final int MAX_VISIBLE = 8;

    private static final int[] ST_TITLE = new int[] {
            TextId.MUSIC_TITLE_TITLE,
            TextId.MUSIC_TITLE_ST1_MID,
            TextId.MUSIC_TITLE_ST1_BOSS,
            TextId.MUSIC_TITLE_ST2_MID,
            TextId.MUSIC_TITLE_ST2_BOSS,
            TextId.MUSIC_TITLE_ST3_MID,
            TextId.MUSIC_TITLE_ST3_BOSS,
            TextId.MUSIC_TITLE_ST4_MID,
            TextId.MUSIC_TITLE_ST4_BOSS,
            TextId.MUSIC_TITLE_ST5_MID,
            TextId.MUSIC_TITLE_ST5_BOSS,
            TextId.MUSIC_TITLE_ST6_MID,
            TextId.MUSIC_TITLE_ST6_BOSS,
            TextId.MUSIC_TITLE_EXTRA_MID,
            TextId.MUSIC_TITLE_EXTRA_BOSS,
            TextId.MUSIC_TITLE_EPILOGUE,
            TextId.MUSIC_TITLE_ENDING,
            TextId.MUSIC_TITLE_LASTWORD };

    private static final int[] ST_ORIG = new int[] {
            TextId.MUSIC_ORIG_01,
            TextId.MUSIC_ORIG_02,
            TextId.MUSIC_ORIG_03,
            TextId.MUSIC_ORIG_04,
            TextId.MUSIC_ORIG_05,
            TextId.MUSIC_ORIG_06,
            TextId.MUSIC_ORIG_07,
            TextId.MUSIC_ORIG_08,
            TextId.MUSIC_ORIG_09,
            TextId.MUSIC_ORIG_10,
            TextId.MUSIC_ORIG_11,
            TextId.MUSIC_ORIG_12,
            TextId.MUSIC_ORIG_13,
            TextId.MUSIC_ORIG_14,
            TextId.MUSIC_ORIG_15,
            TextId.MUSIC_ORIG_16,
            TextId.MUSIC_ORIG_17,
            TextId.MUSIC_ORIG_18 };

    private static final int[] ST_FROM_GAME = new int[] {
            TextId.MUSIC_FROM_01,
            TextId.MUSIC_FROM_02,
            TextId.MUSIC_FROM_03,
            TextId.MUSIC_FROM_04,
            TextId.MUSIC_FROM_05,
            TextId.MUSIC_FROM_06,
            TextId.MUSIC_FROM_07,
            TextId.MUSIC_FROM_08,
            TextId.MUSIC_FROM_09,
            TextId.MUSIC_FROM_10,
            TextId.MUSIC_FROM_11,
            TextId.MUSIC_FROM_12,
            TextId.MUSIC_FROM_13,
            TextId.MUSIC_FROM_14,
            TextId.MUSIC_FROM_15,
            TextId.MUSIC_FROM_16,
            TextId.MUSIC_FROM_17,
            TextId.MUSIC_FROM_18 };

    private int selectedTrack;
    private int stVolume;

    private int[] opt;

    public void enter() {
        opt = GameOptions.load();

        selectedTrack = 0;
        stVolume = MusicRoomSettings.loadVolume(opt[1]);
    }

    public int getVolume() {
        return stVolume;
    }

    public Result update(int pressed) {
        if (UiInput.cancel(pressed)) {
            selectedTrack = 0;
            MusicRoomSettings.saveVolume(stVolume);
            return new Result(Result.KIND_BACK_TO_TITLE);
        }

        if (UiInput.confirm(pressed)) {
            GameProgress.INSTANCE.markMusicPlayed(selectedTrack);
            return new Result(Result.KIND_PLAY_TRACK, selectedTrack);
        }

        if (UiInput.down(pressed)) {
            selectedTrack = MenuCursor.move(selectedTrack, 1, SOUNDTEST_MAX);
            return null;
        }

        if (UiInput.up(pressed)) {
            selectedTrack = MenuCursor.move(selectedTrack, -1, SOUNDTEST_MAX);
            return null;
        }

        if (UiInput.left(pressed)) {
            if (stVolume != 0) {
                stVolume -= 10;
                if (stVolume < 0) {
                    stVolume = 0;
                }
            }
            MusicRoomSettings.saveVolume(stVolume);
            return new Result(Result.KIND_VOLUME_CHANGED, stVolume);
        }

        if (UiInput.right(pressed)) {
            if (stVolume != 100) {
                stVolume += 10;
                if (stVolume > 100) {
                    stVolume = 100;
                }
            }
            MusicRoomSettings.saveVolume(stVolume);
            return new Result(Result.KIND_VOLUME_CHANGED, stVolume);
        }

        return null;
    }

    public void render(Graphics g) {
        if (g == null) {
            return;
        }

        UiDraw.drawGradation(g, 0, 0, 240, 240, 0x000000, 0xFF9191, 32);

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        UiDraw.drawString2(g, font, UiText.get(TextId.MUSICROOM_TITLE), 120, 22, 1, 0xFFFFFF, 0x333333);

        int visibleStart = MenuCursor.trailingVisibleStart(selectedTrack, SOUNDTEST_MAX, MAX_VISIBLE);
        int visibleEnd = visibleStart + MAX_VISIBLE;
        for (int i = visibleStart; i < visibleEnd; i++) {
            int rel = i - visibleStart;
            UiDraw.drawString2(g, font, UiText.get(ST_TITLE[i]), 20 + rel * 3, 45 + 15 * rel, 0, 0x777777, 0x333333);
        }

        int n = selectedTrack;
        int relSel = n - visibleStart;
        UiDraw.drawString2(g, font, UiText.get(ST_TITLE[n]), 20 + relSel * 3, 45 + 15 * relSel, 0, 0xFFFFFF, 0x777777);

        g.setColor(0x333333);
        g.fillRect(10, 160, 220, 70);
        g.setColor(0xFF3333);
        g.drawRect(10, 160, 220, 70);

        String prefix;
        if (n < 10) {
            prefix = "music0" + n + ":";
        } else {
            prefix = "music" + n + ":";
        }

        boolean played = (n >= 0 && n < GameProgress.MUSIC_COUNT && GameProgress.INSTANCE.isMusicPlayed(n));
        String line0;
        String line1;
        String line2;
        if (played) {
            line0 = prefix + UiText.get(ST_TITLE[n]);
            line1 = UiText.get(ST_ORIG[n]);
            line2 = UiText.get(TextId.MUSICROOM_FROM_PREFIX) + " " + UiText.get(ST_FROM_GAME[n]);
        } else {
            line0 = prefix + UiText.get(TextId.MUSIC_LOCK_TITLE);
            line1 = UiText.get(TextId.MUSIC_LOCK_LINE1);
            line2 = UiText.get(TextId.MUSIC_LOCK_LINE2);
        }

        UiDraw.drawStringPlain(g, font, line0, 15, 176, 0, 0xFFFFFF);
        UiDraw.drawStringPlain(g, font, line1, 15, 195, 0, 0xFFFFFF);
        UiDraw.drawStringPlain(g, font, line2, 15, 210, 0, 0xFFFFFF);

        UiDraw.drawStringPlain(g, font, UiText.get(TextId.MUSIC_VOLUME) + stVolume + UiText.get(TextId.MUSIC_PERCENRAGE), 155, 225, 0, 0xFF3333);
    }
}
