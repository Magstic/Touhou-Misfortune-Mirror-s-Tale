package touhou;

/** Targeting projection of the enemy state owned by StageRuntime. It does not allocate enemies. */
public final class EnemyTargetRegistry {
    public static final int CAPACITY = 32;

    private final boolean[] active = new boolean[CAPACITY];
    private final boolean[] targetable = new boolean[CAPACITY];
    private final int[] x = new int[CAPACITY];
    private final int[] y = new int[CAPACITY];
    private final int[] aabbLeft = new int[CAPACITY];
    private final int[] aabbRight = new int[CAPACITY];
    private final int[] aabbTop = new int[CAPACITY];
    private final int[] aabbBottom = new int[CAPACITY];

    private static final int TARGET_SCAN_X_MIN = 0;
    private static final int TARGET_SCAN_X_MAX = 12713984;
    private static final int TARGET_SCAN_Y_MIN = 524288;
    private static final int TARGET_SCAN_Y_MAX = 15335424;

    public int capacity() {
        return CAPACITY;
    }

    public void clear() {
        for (int i = 0; i < CAPACITY; i++) {
            clearSlot(i);
        }
    }

    public boolean remove(int index) {
        requireIndex(index);
        boolean removed = active[index];
        clearSlot(index);
        return removed;
    }

    public boolean isActive(int index) {
        return index >= 0 && index < CAPACITY && active[index];
    }

    public boolean isTargetable(int index) {
        return index >= 0 && index < CAPACITY && active[index] && targetable[index];
    }

    public int getXFixed(int index) {
        requireIndex(index);
        return x[index];
    }

    public int getYFixed(int index) {
        requireIndex(index);
        return y[index];
    }

    public int getAabbLeftFixed(int index) {
        requireIndex(index);
        return aabbLeft[index];
    }

    public int getAabbRightFixed(int index) {
        requireIndex(index);
        return aabbRight[index];
    }

    public int getAabbTopFixed(int index) {
        requireIndex(index);
        return aabbTop[index];
    }

    public int getAabbBottomFixed(int index) {
        requireIndex(index);
        return aabbBottom[index];
    }

    public void put(int index, int xFixed, int yFixed,
            int leftExtentFixed, int rightExtentFixed, int topExtentFixed, int bottomExtentFixed,
            boolean canTarget) {
        requireIndex(index);
        active[index] = true;
        x[index] = xFixed;
        y[index] = yFixed;
        targetable[index] = canTarget;
        aabbLeft[index] = xFixed - leftExtentFixed;
        aabbRight[index] = xFixed + rightExtentFixed;
        aabbTop[index] = yFixed - topExtentFixed;
        aabbBottom[index] = yFixed + bottomExtentFixed;
    }

    public void setTargetable(int index, boolean canTarget) {
        requireIndex(index);
        if (!active[index]) {
            throw new IllegalArgumentException("Inactive enemy target index: " + index);
        }
        targetable[index] = canTarget;
    }

    public int findNearestTargetByX(int xFixed) {
        int best = -1;
        int bestAbsDx = 0;
        for (int i = 0; i < CAPACITY; i++) {
            if (!isTargetable(i)) {
                continue;
            }
            if (!(aabbLeft[i] < TARGET_SCAN_X_MAX && aabbTop[i] < TARGET_SCAN_Y_MAX
                    && aabbRight[i] > TARGET_SCAN_X_MIN && aabbBottom[i] > TARGET_SCAN_Y_MIN)) {
                continue;
            }
            int dx = xFixed - x[i];
            if (dx < 0) {
                dx = -dx;
            }
            if (best == -1 || dx < bestAbsDx) {
                best = i;
                bestAbsDx = dx;
            }
        }
        return best;
    }

    private void clearSlot(int index) {
        active[index] = false;
        targetable[index] = false;
        x[index] = 0;
        y[index] = 0;
        aabbLeft[index] = 0;
        aabbRight[index] = 0;
        aabbTop[index] = 0;
        aabbBottom[index] = 0;
    }

    private static void requireIndex(int index) {
        if (index < 0 || index >= CAPACITY) {
            throw new IllegalArgumentException("Invalid enemy target index: " + index);
        }
    }
}
