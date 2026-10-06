package touhou;

public final class ResourcePaths {
    public static final String CC_TABLE = "/cc.dat";
    public static final String STAGE_DATA = "/stage.dat";
    public static final String BGM_PACK = "/snd/bgm.dat";

    private ResourcePaths() {
    }

    public static String sprite(int imageIndex) {
        if (imageIndex < 0 || imageIndex > 999) {
            throw new IllegalArgumentException("imageIndex");
        }
        if (imageIndex < 10) {
            return "/sp/00" + imageIndex + ".gif";
        }
        if (imageIndex < 100) {
            return "/sp/0" + imageIndex + ".gif";
        }
        return "/sp/" + imageIndex + ".gif";
    }

    public static String languagePack(String languageCode) {
        requireSimpleSegment(languageCode, "languageCode");
        return "/i18n/" + languageCode + ".dat";
    }

    public static String localized(String languageCode, String relativePath) {
        requireSimpleSegment(languageCode, "languageCode");
        requireRelativePath(relativePath);
        return "/i18n/" + languageCode + "/" + relativePath;
    }

    static void requireCanonical(String path) {
        if (path == null || !path.startsWith("/") || path.endsWith("/") || path.indexOf("//") >= 0
                || path.indexOf('\\') >= 0 || path.indexOf("..") >= 0 || path.indexOf("/./") >= 0) {
            throw new IllegalArgumentException("Non-canonical resource path: " + path);
        }
    }

    private static void requireSimpleSegment(String value, String name) {
        if (value == null || value.length() == 0 || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.indexOf("..") >= 0) {
            throw new IllegalArgumentException(name);
        }
    }

    private static void requireRelativePath(String value) {
        if (value == null || value.length() == 0 || value.startsWith("/") || value.indexOf('\\') >= 0 || value.indexOf("..") >= 0) {
            throw new IllegalArgumentException("relativePath");
        }
    }
}
