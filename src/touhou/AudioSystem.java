package touhou;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

public final class AudioSystem implements PlayerListener {

    private static final int STATE_UNREALIZED = 100;
    private static final int STATE_PREFETCHED = 300;

    // Offsets and lengths are read lazily from the BGMP file header.
    private BgmPack midiPack = new BgmPack(ResourcePaths.BGM_PACK);
    private boolean heavyBgmEnabled = true;

    // Some real devices have expensive JSR-135 Player.getState().
    // When native looping is unavailable, we only need to poll state periodically
    // to detect end-of-media and restart looping tracks.
    private static final int BGM_STATE_POLL_INTERVAL_FRAMES = 8;
    // When PlayerListener is active, getState() is only a safety net;
    // END_OF_MEDIA event drives transitions. Poll very infrequently.
    private static final int BGM_STATE_POLL_INTERVAL_FRAMES_WITH_LISTENER = 120;

    // Runtime toggle (Battle Pause screen). When disabled, never create Player.
    private boolean bgmEnabled = true;

    private int bgmVol = 50;
    private int seVol = 50;

    // Music Room can temporarily override BGM volume without touching Options.
    private boolean bgmVolOverrideActive;
    private int bgmVolOverride = 50;

    private final SoundEffectSystem se = new SoundEffectSystem();

    private int desiredTrack = -1;
    private boolean desiredLoop = true;

    private int currentTrack = -1;
    private boolean currentLoop = true;

    private boolean phaseIntro;
    private boolean phaseStarted;

    private int introEntry = -1;
    private int loopEntry = -1;
    private int singleEntry = -1;

    private Player player;

    // Some JSR-135 implementations require keeping the InputStream open for the lifetime of Player.
    private InputStream playerStream;

    private boolean nativeLoopActive;

    private boolean bgmPaused;
    private long bgmPausedMediaTime;
    private boolean bgmPausedMediaTimeValid;

    private int bgmStatePollCnt;

    private boolean playerListenerActive;
    private boolean bgmEndOfMediaPending;

    // Pre-created loop Player: built during intro playback to avoid mid-gameplay stutter.
    private Player pendingLoopPlayer;
    private InputStream pendingLoopStream;
    private boolean pendingLoopNativeLoop;
    private boolean pendingLoopListenerActive;

    public void setHeavyBgmEnabled(boolean enabled) {
        if (heavyBgmEnabled == enabled) {
            return;
        }
        heavyBgmEnabled = enabled;
        if (BgmCatalog.isBattleTrack(currentTrack)) {
            stopBgm();
        }
    }

    private void selectPack(int trackId) {
        String path = !heavyBgmEnabled && BgmCatalog.isBattleTrack(trackId)
                ? ResourcePaths.BGM_LITE_PACK : ResourcePaths.BGM_PACK;
        if (midiPack.resourcePath.equals(path)) {
            return;
        }
        midiPack = new BgmPack(path);
    }

    public void toggleBgmEnabled() {
        setBgmEnabled(!bgmEnabled);
    }

    public void setBgmEnabled(boolean enabled) {
        if (bgmEnabled == enabled) {
            return;
        }
        bgmEnabled = enabled;
        if (!bgmEnabled) {
            stopBgm();
        }
    }

    public void setVolumes(int bgmVol, int seVol) {
        int nb = clamp100(bgmVol);
        int ns = clamp100(seVol);
        if (this.bgmVol == nb && this.seVol == ns) {
            return;
        }

        this.bgmVol = nb;
        this.seVol = ns;

        se.setVolume(this.seVol);

        int eff = effectiveBgmVol();

        if (eff == 0) {
            stopBgm();
        } else {
            applyVolumeSafe(player, eff);
        }
    }

    // Apply Music Room BGM volume override.
    public void setBgmVolumeOverride(int bgmVol) {
        bgmVolOverrideActive = true;
        bgmVolOverride = clamp100(bgmVol);
        applyVolumeSafe(player, effectiveBgmVol());
    }

    public void clearBgmVolumeOverride() {
        if (!bgmVolOverrideActive) {
            return;
        }
        bgmVolOverrideActive = false;
        applyVolumeSafe(player, effectiveBgmVol());
    }

    public void setSeEnabledMask14(int mask) {
        se.setEnabledMask(mask & 0x3FFF);
    }

    public void requestBgm(int trackId, boolean loop) {
        if (trackId < 0) {
            if (desiredTrack < 0 && currentTrack < 0 && player == null && !bgmPaused) {
                return;
            }

            desiredTrack = -1;
            desiredLoop = loop;
            stopBgm();
            return;
        }
        if (!BgmCatalog.isTrack(trackId)) {
            throw new IllegalArgumentException("trackId=" + trackId);
        }
        if (desiredTrack == trackId && desiredLoop == loop) {
            return;
        }
        desiredTrack = trackId;
        desiredLoop = loop;
    }

    public void stopBgm() {
        closePendingLoopPlayer();
        closePlayer();

        currentTrack = -1;
        phaseIntro = false;
        phaseStarted = false;
        introEntry = -1;
        loopEntry = -1;
        singleEntry = -1;
        bgmPaused = false;
        bgmPausedMediaTimeValid = false;
        bgmStatePollCnt = 0;
        playerListenerActive = false;
        bgmEndOfMediaPending = false;
    }

    public void stopAll() {
        stopBgm();
        se.stop();
    }

    public void playSeType(int seId) {
        se.play(seId);
    }

    // Call once per frame to drive polling-based playback and intro->loop transitions.
    public void tick() {
        if (effectiveBgmVol() == 0) {
            return;
        }
        if (desiredTrack < 0) {
            return;
        }

        if (desiredTrack != currentTrack || desiredLoop != currentLoop) {
            switchToTrack(desiredTrack, desiredLoop);
        }

        if (player == null) {
            ensurePlayerCreated();
            if (player == null) {
                return;
            }
        }

        if (bgmPaused) {
            return;
        }

        // Prefer end-of-media event when supported; fall back to low-frequency polling.
        if (bgmEndOfMediaPending) {
            bgmEndOfMediaPending = false;
            bgmStatePollCnt = 0;

            if (phaseStarted) {
                if (phaseIntro) {
                    if (!switchFromIntroToLoopOrSingle()) {
                        stopBgm();
                        return;
                    }
                    return;
                }
                if (!nativeLoopActive && currentLoop) {
                    tryStart();
                    return;
                }
            }
        }

        if (nativeLoopActive && phaseStarted && !phaseIntro) {
            return;
        }

        // Throttle expensive getState() polling during BGM playback.
        // Applies to both intro and loop phases (except native loop which returns above).
        if (phaseStarted && !nativeLoopActive) {
            int interval = playerListenerActive ? BGM_STATE_POLL_INTERVAL_FRAMES_WITH_LISTENER
                    : BGM_STATE_POLL_INTERVAL_FRAMES;
            bgmStatePollCnt++;
            if (bgmStatePollCnt < interval) {
                return;
            }
            bgmStatePollCnt = 0;
        } else {
            bgmStatePollCnt = 0;
        }

        int st;
        try {
            st = player.getState();
        } catch (Throwable t) {
            closePlayer();
            return;
        }

        if (st == STATE_UNREALIZED || st == STATE_PREFETCHED) {
            // If intro was started before and we are back to PREFETCHED, treat as end-of-media.
            if (phaseIntro && phaseStarted && st == STATE_PREFETCHED) {
                // Intro ended -> switch to loop/single.
                if (!switchFromIntroToLoopOrSingle()) {
                    // No loop/single available, stop.
                    stopBgm();
                    return;
                }
                // Re-evaluate state after switching.
                return;
            }

            // Loop ended -> restart only when looping is enabled.
            if (!phaseIntro && phaseStarted && st == STATE_PREFETCHED) {
                if (nativeLoopActive) {
                    return;
                }
                if (!currentLoop) {
                    return;
                }
            }

            tryStart();
        }
    }

    private void switchToTrack(int trackId, boolean loop) {
        closePendingLoopPlayer();
        closePlayer();
        selectPack(trackId);

        currentTrack = trackId;
        currentLoop = loop;

        resolveTrackEntries(trackId);
        phaseIntro = introEntry >= 0;
        phaseStarted = false;

        bgmStatePollCnt = 0;
        bgmEndOfMediaPending = false;
    }

    private void resolveTrackEntries(int trackId) {
        int base = trackId * BgmCatalog.PART_COUNT;
        introEntry = resolvePackEntry(base + BgmCatalog.PART_INTRO);
        loopEntry = resolvePackEntry(base + BgmCatalog.PART_LOOP);
        singleEntry = resolvePackEntry(base + BgmCatalog.PART_SINGLE);
    }

    private boolean switchFromIntroToLoopOrSingle() {
        closePlayer();
        phaseIntro = false;
        phaseStarted = false;

        // Use pre-created loop player if available (avoids mid-gameplay stutter).
        if (pendingLoopPlayer != null) {
            player = pendingLoopPlayer;
            playerStream = pendingLoopStream;
            nativeLoopActive = pendingLoopNativeLoop;
            playerListenerActive = pendingLoopListenerActive;
            pendingLoopPlayer = null;
            pendingLoopStream = null;
            pendingLoopNativeLoop = false;
            pendingLoopListenerActive = false;
            return true;
        }

        // Create synchronously when no pre-created player is available.
        createPlayerFromEntry(loopOrSingleEntry());
        return player != null;
    }

    private void ensurePlayerCreated() {
        if (player != null) {
            return;
        }

        // When BGM is OFF, do not create Player (avoid unnecessary overhead/stutters).
        if (effectiveBgmVol() == 0) {
            return;
        }

        createPlayerFromEntry(phaseIntro ? introEntry : loopOrSingleEntry());

        // Pre-create loop player while intro plays, so intro->loop switch is instant.
        if (player != null && phaseIntro) {
            precreateLoopPlayer();
        }
    }

    private int loopOrSingleEntry() {
        if (!currentLoop && singleEntry >= 0) {
            return singleEntry;
        }
        return loopEntry >= 0 ? loopEntry : singleEntry;
    }

    private int resolvePackEntry(int entryIndex) {
        return midiPack.hasEntry(entryIndex) ? entryIndex : -1;
    }

    private void createPlayerFromEntry(int entry) {
        if (effectiveBgmVol() == 0 || entry < 0) {
            return;
        }

        InputStream is = null;
        try {
            is = midiPack.openEntry(entry);
            if (is == null) {
                return;
            }

            Player p = Manager.createPlayer(is, "audio/midi");
            player = p;
            playerStream = is;
            is = null;

            playerListenerActive = false;
            try {
                player.addPlayerListener(this);
                playerListenerActive = true;
            } catch (Throwable ignore) {
                playerListenerActive = false;
            }

            nativeLoopActive = false;
            try {
                player.prefetch();
            } catch (Throwable ignore) {
            }

            boolean wantNativeLoop = !phaseIntro && currentLoop;
            if (wantNativeLoop) {
                try {
                    player.setLoopCount(-1);
                    nativeLoopActive = true;
                } catch (Throwable ignore) {
                    nativeLoopActive = false;
                }
            } else {
                try {
                    player.setLoopCount(1);
                } catch (Throwable ignore) {
                }
            }

            applyVolumeSafe(player, effectiveBgmVol());
        } catch (Throwable t) {
            closePlayer();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream in;
        private int remaining;

        LimitedInputStream(InputStream in, int remaining) {
            this.in = in;
            this.remaining = remaining;
        }

        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = in.read();
            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int want = len;
            if (want > remaining) {
                want = remaining;
            }
            int n = in.read(b, off, want);
            if (n > 0) {
                remaining -= n;
            }
            return n;
        }

        public long skip(long n) throws IOException {
            if (remaining <= 0) {
                return 0;
            }
            long want = n;
            if (want > remaining) {
                want = remaining;
            }
            long s = in.skip(want);
            if (s > 0) {
                remaining -= (int) s;
            }
            return s;
        }

        public int available() throws IOException {
            int a = in.available();
            if (a > remaining) {
                return remaining;
            }
            return a;
        }

        public void close() throws IOException {
            in.close();
        }
    }

    /**
     * Reads offset/length tables from the BGMP pack file header on first access.
     * Header layout (big-endian):
     *   4B magic "BGMP", 2B version, 1B codec, 1B partCount,
     *   2B trackCount, 2B entryCount, 4B reserved,
     *   entryCount*4B offsets, entryCount*4B lengths.
     */
    private static final class BgmPack {
        final String resourcePath;

        // Lazily populated from file header
        private int[] offsets;
        private int[] lengths;
        private boolean headerLoaded;
        private boolean headerFailed;

        BgmPack(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        private void ensureHeader() {
            if (headerLoaded || headerFailed) {
                return;
            }
            InputStream raw = null;
            try {
                raw = ResourceLoader.openOptional(resourcePath);
                if (raw == null) {
                    headerFailed = true;
                    return;
                }
                DataInputStream dis = new DataInputStream(raw);

                // Validate magic
                int m = dis.readInt();
                if (m != BgmCatalog.PACK_MAGIC) {
                    headerFailed = true;
                    return;
                }

                int version = dis.readShort() & 0xFFFF;
                int codec = dis.readByte() & 0xFF;
                int partCount = dis.readByte() & 0xFF;
                int trackCount = dis.readShort() & 0xFFFF;
                int entryCount = dis.readShort() & 0xFFFF;
                int reserved = dis.readInt();
                if (version != BgmCatalog.PACK_VERSION || codec != BgmCatalog.CODEC_MIDI || partCount != BgmCatalog.PART_COUNT
                        || trackCount != BgmCatalog.TRACK_COUNT
                        || entryCount != BgmCatalog.TRACK_COUNT * BgmCatalog.PART_COUNT || reserved != 0) {
                    headerFailed = true;
                    return;
                }

                offsets = new int[entryCount];
                lengths = new int[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    offsets[i] = dis.readInt();
                }
                for (int i = 0; i < entryCount; i++) {
                    lengths[i] = dis.readInt();
                    if (lengths[i] < 0 || (lengths[i] == 0 && offsets[i] != -1) || (lengths[i] > 0 && offsets[i] < 0)) {
                        headerFailed = true;
                        return;
                    }
                }
                int dataPosition = BgmCatalog.HEADER_SIZE + entryCount * BgmCatalog.ENTRY_SIZE;
                for (int i = 0; i < entryCount; i++) {
                    if (lengths[i] == 0) {
                        continue;
                    }
                    if (offsets[i] != dataPosition || dataPosition > Integer.MAX_VALUE - lengths[i]) {
                        headerFailed = true;
                        return;
                    }
                    dataPosition += lengths[i];
                }
                headerLoaded = true;
            } catch (Throwable t) {
                headerFailed = true;
            } finally {
                if (raw != null) {
                    try {
                        raw.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }

        boolean hasEntry(int entryIndex) {
            ensureHeader();
            return headerLoaded && entryIndex >= 0 && entryIndex < lengths.length && lengths[entryIndex] > 0;
        }

        InputStream openEntry(int entryIndex) throws IOException {
            if (!hasEntry(entryIndex)) {
                return null;
            }

            InputStream raw = ResourceLoader.openOptional(resourcePath);
            if (raw == null) {
                return null;
            }

            boolean ok = false;
            try {
                skipFully(raw, offsets[entryIndex]);
                InputStream lim = new LimitedInputStream(raw, lengths[entryIndex]);
                ok = true;
                return lim;
            } finally {
                if (!ok) {
                    try {
                        raw.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }

    private static void skipFully(InputStream in, int bytes) throws IOException {
        int remaining = bytes;
        while (remaining > 0) {
            long s = in.skip(remaining);
            if (s <= 0) {
                int b = in.read();
                if (b < 0) {
                    throw new IOException("EOF while skipping");
                }
                remaining--;
            } else {
                remaining -= (int) s;
            }
        }
    }

    private void tryStart() {
        if (player == null) {
            return;
        }
        try {
            player.start();
            phaseStarted = true;
            applyVolumeSafe(player, effectiveBgmVol());
        } catch (Throwable t) {
            closePlayer();
        }
    }

    // Pre-create the loop/single Player so intro->loop transition is stutter-free.
    private void precreateLoopPlayer() {
        if (pendingLoopPlayer != null) {
            return;
        }
        int entry = loopOrSingleEntry();
        if (entry < 0) {
            return;
        }

        InputStream is = null;
        try {
            is = midiPack.openEntry(entry);
            if (is == null) {
                return;
            }

            Player p = Manager.createPlayer(is, "audio/midi");
            pendingLoopPlayer = p;
            pendingLoopStream = is;
            is = null;

            pendingLoopListenerActive = false;
            try {
                pendingLoopPlayer.addPlayerListener(this);
                pendingLoopListenerActive = true;
            } catch (Throwable ignore) {
            }

            try {
                pendingLoopPlayer.prefetch();
            } catch (Throwable ignore) {
            }

            pendingLoopNativeLoop = false;
            boolean wantNativeLoop = currentLoop;
            if (wantNativeLoop) {
                try {
                    pendingLoopPlayer.setLoopCount(-1);
                    pendingLoopNativeLoop = true;
                } catch (Throwable ignore) {
                }
            } else {
                try {
                    pendingLoopPlayer.setLoopCount(1);
                } catch (Throwable ignore) {
                }
            }

            applyVolumeSafe(pendingLoopPlayer, effectiveBgmVol());
        } catch (Throwable t) {
            closePendingLoopPlayer();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private void closePendingLoopPlayer() {
        if (pendingLoopPlayer == null) {
            return;
        }
        InputStream ps = pendingLoopStream;
        pendingLoopStream = null;
        try {
            pendingLoopPlayer.removePlayerListener(this);
        } catch (Throwable ignore) {
        }
        try {
            pendingLoopPlayer.stop();
        } catch (Throwable ignore) {
        }
        try {
            pendingLoopPlayer.close();
        } catch (Throwable ignore) {
        }
        pendingLoopPlayer = null;
        pendingLoopNativeLoop = false;
        pendingLoopListenerActive = false;
        if (ps != null) {
            try {
                ps.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private void closePlayer() {
        if (player == null) {
            return;
        }
        InputStream ps = playerStream;
        playerStream = null;
        try {
            player.removePlayerListener(this);
        } catch (Throwable ignore) {
        }

        try {
            player.stop();
        } catch (Throwable ignore) {
        }
        try {
            player.close();
        } catch (Throwable ignore) {
        }
        player = null;
        nativeLoopActive = false;
        playerListenerActive = false;
        bgmEndOfMediaPending = false;

        if (ps != null) {
            try {
                ps.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static void applyVolumeSafe(Player p, int vol) {
        if (p == null) {
            return;
        }
        int v = clamp100(vol);
        try {
            VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
            if (vc != null) {
                vc.setLevel(v);
            }
        } catch (Throwable ignore) {
        }
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

    private int effectiveBgmVol() {
        if (!bgmEnabled) {
            return 0;
        }
        return bgmVolOverrideActive ? bgmVolOverride : bgmVol;
    }

    // Pause BGM without closing player. Best effort resume via mediaTime.
    public void pauseBgm() {
        if (bgmPaused) {
            return;
        }
        if (player == null) {
            bgmPaused = true;
            bgmPausedMediaTimeValid = false;
            return;
        }
        bgmPaused = true;
        bgmPausedMediaTimeValid = false;
        bgmEndOfMediaPending = false;
        try {
            bgmPausedMediaTime = player.getMediaTime();
            bgmPausedMediaTimeValid = true;
        } catch (Throwable ignore) {
        }
        try {
            player.stop();
        } catch (Throwable ignore) {
        }
    }

    // Resume BGM from paused position if supported.
    public void resumeBgm() {
        if (!bgmPaused) {
            return;
        }
        bgmPaused = false;
        bgmEndOfMediaPending = false;

        if (player == null) {
            // Recreate player if needed.
            ensurePlayerCreated();
        }
        if (player == null) {
            return;
        }

        applyVolumeSafe(player, effectiveBgmVol());

        if (bgmPausedMediaTimeValid) {
            try {
                player.setMediaTime(bgmPausedMediaTime);
            } catch (Throwable ignore) {
            }
        }
        tryStart();
    }

    public void pauseAll() {
        pauseBgm();
        se.stop();
    }

    public void resumeAll() {
        resumeBgm();
    }

    public void playerUpdate(Player player, String event, Object eventData) {
        if (player == null || player != this.player) {
            return;
        }
        if (PlayerListener.END_OF_MEDIA.equals(event)) {
            bgmEndOfMediaPending = true;
        }
    }
}
