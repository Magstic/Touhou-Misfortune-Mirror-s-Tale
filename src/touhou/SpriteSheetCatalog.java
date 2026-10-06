package touhou;

public final class SpriteSheetCatalog {
    public static final int IMAGE_COUNT = 96;
    public static final int LOGICAL_CANVAS_SIZE = 240;

    private static final SpriteSheetLayout[] LAYOUTS = new SpriteSheetLayout[IMAGE_COUNT];

    static {
        for (int i = 0; i < LAYOUTS.length; i++) {
            LAYOUTS[i] = fullCanvas(ResourcePaths.sprite(i));
        }

        // Logical image 004 intentionally shares the original 003 sheet.
        LAYOUTS[4] = fullCanvas(ResourcePaths.sprite(3));
    }

    private SpriteSheetCatalog() {
    }

    public static SpriteSheetLayout get(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= LAYOUTS.length) {
            throw new IllegalArgumentException("imageIndex");
        }
        return LAYOUTS[imageIndex];
    }

    private static SpriteSheetLayout fullCanvas(String path) {
        return new SpriteSheetLayout(path, 0, 0, LOGICAL_CANVAS_SIZE, LOGICAL_CANVAS_SIZE);
    }
}
