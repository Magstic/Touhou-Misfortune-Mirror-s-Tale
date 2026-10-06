package touhou;

/** Owns lifecycle/gameplay clocks and translates events into serialized progress commands. */
public final class ResultStats {
    public static final ResultStats INSTANCE = new ResultStats();

    private long playtimeStartMs;
    private long boottimeStartMs;
    private boolean playActive;

    private ResultStats() {
    }

    public synchronized void onAppStart() {
        GameProgress.INSTANCE.initialize();
        if (boottimeStartMs != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        boottimeStartMs = now;
        playtimeStartMs = playActive ? now : 0;
    }

    public synchronized void onAppPauseOrStop() {
        commitPlaytime();
        commitBootTime();
        playtimeStartMs = 0;
        boottimeStartMs = 0;
    }

    public synchronized void onReturnToTitle() {
        commitBootTime();
    }

    public synchronized int getBootTimeSecondsNow() {
        long seconds = GameProgress.INSTANCE.getUserStat(GameProgress.UD_TOTAL_BOOT_TIME_SEC);
        if (boottimeStartMs != 0) {
            long elapsed = System.currentTimeMillis() - boottimeStartMs;
            if (elapsed > 0) {
                seconds += elapsed / 1000L;
            }
        }
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    public synchronized void onRunStart(boolean replayActive, int gamemode, int level) {
        if (replayActive) {
            return;
        }
        if (gamemode == 0) {
            GameProgress.INSTANCE.incPlayCountNormalByDifficulty(level);
        } else if (gamemode == 2) {
            GameProgress.INSTANCE.incPlayCountPracticeByDifficulty(level);
        } else if (gamemode == 1) {
            GameProgress.INSTANCE.incPlayCountExtra();
        }
        startPlaytime();
    }

    public synchronized void onSpellPracticeStart(boolean replayActive) {
        if (!replayActive) {
            startPlaytime();
        }
    }

    public synchronized void onEnterEnding(boolean replayActive) {
        if (!replayActive) {
            commitPlaytime();
            GameProgress.INSTANCE.incClearCount();
        }
    }

    public synchronized void onRetry(boolean replayActive) {
        if (!replayActive) {
            GameProgress.INSTANCE.incRetryCount();
            commitPlaytime();
        }
    }

    public synchronized void onRunEnd(boolean replayActive) {
        if (!replayActive) {
            commitPlaytime();
            playActive = false;
            playtimeStartMs = 0;
        }
    }

    private void startPlaytime() {
        commitPlaytime();
        playActive = true;
        // An in-flight game event after lifecycle pause must not start a background clock.
        playtimeStartMs = boottimeStartMs != 0 ? System.currentTimeMillis() : 0;
    }

    private void commitPlaytime() {
        if (playtimeStartMs == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        GameProgress.INSTANCE.addTotalPlayTimeSeconds(elapsedSeconds(playtimeStartMs, now));
        playtimeStartMs = now;
    }

    private void commitBootTime() {
        if (boottimeStartMs == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        GameProgress.INSTANCE.addTotalBootTimeSeconds(elapsedSeconds(boottimeStartMs, now));
        boottimeStartMs = now;
    }

    private static int elapsedSeconds(long start, long now) {
        long seconds = (now - start) / 1000L;
        if (seconds <= 0) {
            return 0;
        }
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
}
