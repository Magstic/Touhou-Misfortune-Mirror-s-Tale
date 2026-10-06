package touhou.ui;

/** Update-owned progress and speed calculations for replay transfers. */
public final class ReplayTransferProgress {
    private long lastTimeMs;
    private int lastBytes;
    private int bytesPerSecond;

    public void reset() {
        lastTimeMs = 0;
        lastBytes = 0;
        bytesPerSecond = 0;
    }

    public void update(long nowMs, int completedBytes) {
        if (lastTimeMs == 0) {
            lastTimeMs = nowMs;
            lastBytes = completedBytes;
            bytesPerSecond = 0;
            return;
        }
        long elapsed = nowMs - lastTimeMs;
        if (elapsed < 400) {
            return;
        }
        int completed = completedBytes - lastBytes;
        if (completed < 0) {
            completed = 0;
        }
        bytesPerSecond = (int) ((completed * 1000L) / elapsed);
        lastTimeMs = nowMs;
        lastBytes = completedBytes;
    }

    public int getBytesPerSecond() {
        return bytesPerSecond;
    }

    public static int percent(int completed, int total) {
        if (total <= 0 || completed <= 0) {
            return 0;
        }
        if (completed >= total) {
            return 100;
        }
        return (completed * 100) / total;
    }

    public static int barWidth(int completed, int total, int width) {
        if (width <= 0 || total <= 0 || completed <= 0) {
            return 0;
        }
        if (completed >= total) {
            return width;
        }
        return (width * completed) / total;
    }

    public static String formatSpeed(int bytesPerSecond) {
        if (bytesPerSecond <= 0) {
            return "0 B/s";
        }
        int kilobytes10 = (bytesPerSecond * 10) / 1024;
        if (kilobytes10 < 10) {
            return "0." + kilobytes10 + " KB/s";
        }
        if (kilobytes10 < 1000) {
            return (kilobytes10 / 10) + "." + (kilobytes10 % 10) + " KB/s";
        }
        int megabytes10 = kilobytes10 / 1024;
        return (megabytes10 / 10) + "." + (megabytes10 % 10) + " MB/s";
    }
}
