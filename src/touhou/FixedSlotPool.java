package touhou;

/** Deterministic fixed-capacity slot allocator used by battle entity stores. */
public final class FixedSlotPool {
    public static final int NO_SLOT = -1;

    private final boolean[] active;
    private int next;

    public FixedSlotPool(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Slot capacity must be positive");
        }
        active = new boolean[capacity];
    }

    public int capacity() {
        return active.length;
    }

    public boolean isValidIndex(int index) {
        return index >= 0 && index < active.length;
    }

    public boolean isActive(int index) {
        return isValidIndex(index) && active[index];
    }

    public int allocate() {
        return allocateExcluding(null, 0);
    }

    public int allocateExcluding(int[] blockedStamps, int blockedStamp) {
        if (blockedStamps != null && blockedStamps.length != active.length) {
            throw new IllegalArgumentException("Slot stamp length does not match capacity");
        }
        for (int offset = 0; offset < active.length; offset++) {
            int index = next + offset;
            if (index >= active.length) {
                index -= active.length;
            }
            if (active[index]) {
                continue;
            }
            if (blockedStamps != null && blockedStamps[index] == blockedStamp) {
                continue;
            }
            active[index] = true;
            next = index + 1;
            if (next == active.length) {
                next = 0;
            }
            return index;
        }
        return NO_SLOT;
    }

    public boolean release(int index) {
        if (!isValidIndex(index) || !active[index]) {
            return false;
        }
        active[index] = false;
        return true;
    }

    public int activeCount() {
        int count = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                count++;
            }
        }
        return count;
    }

    public void clear() {
        for (int i = 0; i < active.length; i++) {
            active[i] = false;
        }
        next = 0;
    }

    boolean[] activeArray() {
        return active;
    }
}
