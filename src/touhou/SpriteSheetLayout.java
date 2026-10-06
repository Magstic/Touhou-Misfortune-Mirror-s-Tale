package touhou;

public final class SpriteSheetLayout {
    private final String path;
    private final int logicalOriginX;
    private final int logicalOriginY;
    private final int logicalWidth;
    private final int logicalHeight;

    public SpriteSheetLayout(String path, int logicalOriginX, int logicalOriginY, int logicalWidth, int logicalHeight) {
        ResourcePaths.requireCanonical(path);
        if (logicalOriginX < 0 || logicalOriginY < 0 || logicalWidth <= 0 || logicalHeight <= 0) {
            throw new IllegalArgumentException("Invalid sprite sheet layout");
        }
        this.path = path;
        this.logicalOriginX = logicalOriginX;
        this.logicalOriginY = logicalOriginY;
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
    }

    public String getPath() {
        return path;
    }

    public int getLogicalOriginX() {
        return logicalOriginX;
    }

    public int getLogicalOriginY() {
        return logicalOriginY;
    }

    public int getLogicalWidth() {
        return logicalWidth;
    }

    public int getLogicalHeight() {
        return logicalHeight;
    }

    public int toPhysicalSourceX(int logicalX) {
        return logicalX - logicalOriginX;
    }

    public int toPhysicalSourceY(int logicalY) {
        return logicalY - logicalOriginY;
    }

    public int toPhysicalDestinationX(int logicalCanvasX) {
        return logicalCanvasX + logicalOriginX;
    }

    public int toPhysicalDestinationY(int logicalCanvasY) {
        return logicalCanvasY + logicalOriginY;
    }
}
