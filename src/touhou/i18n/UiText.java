package touhou.i18n;

import touhou.ResourceException;

public final class UiText {
    private static String[] table;

    private UiText() {
    }

    public static void load() {
        table = null;
        String[] loaded = I18n.splitLines(I18n.loadText("ui.txt"));
        if (loaded.length != TextId.COUNT) {
            throw new ResourceException("ui.txt lines=" + loaded.length + " expected=" + TextId.COUNT);
        }
        table = loaded;
    }

    public static String get(int id) {
        if (table == null) {
            throw new ResourceException("UiText not loaded");
        }
        if (id < 0 || id >= table.length) {
            throw new IllegalArgumentException("textId=" + id);
        }
        return table[id];
    }
}
