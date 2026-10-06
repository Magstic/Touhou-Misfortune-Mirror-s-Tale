package touhou.i18n;

import touhou.ResourceException;

public final class SpellText {
    public static final int SPELL_COUNT = I18nSchema.SPELL_LINE_COUNT;

    private static String[] names;
    private static String[] comments;

    private SpellText() {
    }

    public static void load() {
        String[] loadedNames = I18n.splitLines(I18n.loadText("spcard.dat"));
        String[] loadedComments = I18n.splitLines(I18n.loadText("sprac.dat"));
        if (loadedNames.length != SPELL_COUNT || loadedComments.length != SPELL_COUNT) {
            throw new ResourceException("Spell text count mismatch");
        }
        names = loadedNames;
        comments = loadedComments;
    }

    public static String getName(int spellId) {
        if (names == null) {
            throw new ResourceException("SpellText not loaded");
        }
        if (spellId < 0 || spellId >= names.length) {
            return null;
        }
        String name = names[spellId];
        return name.length() == 0 ? null : name;
    }

    public static String getComment(int spellId) {
        if (comments == null) {
            throw new ResourceException("SpellText not loaded");
        }
        if (spellId < 0 || spellId >= comments.length) {
            return "";
        }
        return comments[spellId];
    }
}
