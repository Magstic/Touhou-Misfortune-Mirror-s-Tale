package touhou.ui;

/** Shared bounds rules for finite UI cursors. */
public final class MenuCursor {
    private MenuCursor() {
    }

    public static int clamp(int cursor, int itemCount) {
        if (itemCount <= 0 || cursor < 0) {
            return 0;
        }
        if (cursor >= itemCount) {
            return itemCount - 1;
        }
        return cursor;
    }

    public static int move(int cursor, int delta, int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        int current = cursor % itemCount;
        if (current < 0) {
            current += itemCount;
        }
        int step = delta % itemCount;
        int next = (current + step) % itemCount;
        if (next < 0) {
            next += itemCount;
        }
        return next;
    }

    public static int toggleTwo(int cursor) {
        return cursor == 0 ? 1 : 0;
    }

    public static int visibleStart(int cursor, int itemCount, int visibleCount) {
        if (itemCount <= 0 || visibleCount <= 0 || visibleCount >= itemCount) {
            return 0;
        }
        int selected = clamp(cursor, itemCount);
        int start = selected - (visibleCount / 2);
        if (start < 0) {
            return 0;
        }
        int lastStart = itemCount - visibleCount;
        return start > lastStart ? lastStart : start;
    }

    public static int trailingVisibleStart(int cursor, int itemCount, int visibleCount) {
        if (itemCount <= 0 || visibleCount <= 0 || visibleCount >= itemCount) {
            return 0;
        }
        int selected = clamp(cursor, itemCount);
        int start = selected - visibleCount + 1;
        return start < 0 ? 0 : start;
    }
}
