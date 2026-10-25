package touhou.i18n;

import java.io.UnsupportedEncodingException;

import touhou.ResourceException;
import touhou.ResourcePaths;

public final class I18n {
    public static final int LANG_JA = 0;
    public static final int LANG_EN = 1;
    public static final int LANG_ZHT = 2;
    public static final int LANG_ZHS = 3;

    private static final String[] LANG_CODES = new String[] { "ja", "en", "zht", "zhs" };

    private static int languageId = LANG_JA;

    private I18n() {
    }

    public static void setLanguageId(int id) {
        if (id < 0 || id >= LANG_CODES.length) {
            throw new IllegalArgumentException("languageId");
        }
        languageId = id;
    }

    public static int getLanguageId() {
        return languageId;
    }

    public static String getLanguageCode() {
        return LANG_CODES[languageId];
    }

    public static int getLanguageCount() {
        return LANG_CODES.length;
    }

    public static boolean isChinese() {
        return languageId == LANG_ZHT || languageId == LANG_ZHS;
    }

    public static String loadText(String relativePath) {
        return decodeUtf8(loadRequiredBytes(relativePath), ResourcePaths.localized(getLanguageCode(), relativePath));
    }

    public static byte[] loadRequiredBytes(String relativePath) {
        String path = ResourcePaths.localized(getLanguageCode(), relativePath);
        byte[] bytes = I18nPack.readBytes(path);
        if (bytes == null) {
            throw new ResourceException("Missing localized resource: " + path);
        }
        return bytes;
    }

    public static String[] splitLines(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }

        int effectiveLength = text.length();
        while (effectiveLength > 0) {
            char c = text.charAt(effectiveLength - 1);
            if (c != '\n' && c != '\r') {
                break;
            }
            effectiveLength--;
        }
        if (effectiveLength == 0) {
            return new String[0];
        }

        int lineCount = 1;
        for (int i = 0; i < effectiveLength; i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }

        String[] lines = new String[lineCount];
        int line = 0;
        int start = 0;
        for (int i = 0; i <= effectiveLength; i++) {
            if (i < effectiveLength && text.charAt(i) != '\n') {
                continue;
            }
            int end = i;
            if (end > start && text.charAt(end - 1) == '\r') {
                end--;
            }
            lines[line++] = text.substring(start, end);
            start = i + 1;
        }
        return lines;
    }

    private static String decodeUtf8(byte[] bytes, String path) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ResourceException("UTF-8 unavailable for resource: " + path);
        }
    }
}
