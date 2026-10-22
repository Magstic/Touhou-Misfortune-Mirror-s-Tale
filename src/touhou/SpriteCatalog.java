package touhou;

import java.io.DataInputStream;
import java.io.IOException;

/** The complete image and sprite tables, read directly from the final cc.dat. */
public final class SpriteCatalog {
    public static final int NORMAL = 0;
    public static final int MIRROR = 1;
    public static final int HALF = 2;
    public static final int QUARTER = 3;
    private static final int STRIDE = 13;
    private static final int FORMAT_MAGIC = 0x43433031; // 'CC01'
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_IMAGE_COUNT = 32768;
    private static final int PALETTE_COUNT = 6;
    private static final int IMAGE_STRIDE = 4;
    private static final int IMAGE_BASE = 0;
    private static final int IMAGE_PALETTE = 1;
    private static final int IMAGE_W = 2;
    private static final int IMAGE_H = 3;
    private static final int IMAGE = 0;
    private static final int WIDTH = 1;
    private static final int HEIGHT = 2;
    private static final int ANCHOR_X = 3;
    private static final int ANCHOR_Y = 4;
    private static final int PIXEL_X = 5;
    private static final int PIXEL_Y = 6;
    private static final int RENDER_W = 7;
    private static final int RENDER_H = 8;
    private static final int TRIM_X = 9;
    private static final int TRIM_Y = 10;
    private static final int RENDER_MODE = 11;
    private static final int BOTTOM_GAP = 12;
    private static SpriteCatalog instance;
    private final String[] imageNames;
    // Per image: variant base (-1 for a file), palette (-1 for a file), width, height.
    private final short[] images;
    private final short[] data;

    private SpriteCatalog(DataInputStream in) throws IOException {
        if (in.readInt() != FORMAT_MAGIC || in.readUnsignedShort() != FORMAT_VERSION) {
            throw new IOException("Invalid cc.dat header");
        }
        int imageCount = in.readUnsignedShort();
        if (imageCount == 0 || imageCount > MAX_IMAGE_COUNT || in.readUnsignedShort() != SpriteSheetCatalog.SPRITE_COUNT) {
            throw new IOException("Invalid cc.dat counts");
        }
        imageNames = new String[imageCount];
        images = new short[imageNames.length * IMAGE_STRIDE];
        for (int id = 0; id < imageNames.length; id++) {
            String name = in.readUTF();
            imageNames[id] = name;
            int i = id * IMAGE_STRIDE;
            for (int k = 0; k < IMAGE_STRIDE; k++) images[i + k] = in.readShort();
            int base = images[i + IMAGE_BASE], palette = images[i + IMAGE_PALETTE];
            if (images[i + IMAGE_W] <= 0 || images[i + IMAGE_H] <= 0) {
                throw new IOException("Invalid image size: " + id);
            }
            if (base == -1) {
                if (palette != -1 || name.length() == 0 || name.indexOf("..") >= 0 || !name.endsWith(".png")) {
                    throw new IOException("Invalid image file: " + id);
                }
                for (int k = 0; k < name.length(); k++) {
                    char c = name.charAt(k);
                    if (!(c >= '0' && c <= '9') && !(c >= 'A' && c <= 'Z')
                            && !(c >= 'a' && c <= 'z') && c != '_' && c != '.') {
                        throw new IOException("Invalid image filename: " + id);
                    }
                }
                for (int k = 0; k < id; k++) if (name.equals(imageNames[k])) {
                    throw new IOException("Duplicate image file: " + name);
                }
            } else if (base < 0 || base >= id || images[base * IMAGE_STRIDE + IMAGE_BASE] != -1
                    || palette < 0 || palette >= PALETTE_COUNT || name.length() != 0
                    || images[i + IMAGE_W] != images[base * IMAGE_STRIDE + IMAGE_W] || images[i + IMAGE_H] != images[base * IMAGE_STRIDE + IMAGE_H]) {
                throw new IOException("Invalid image variant: " + id);
            }
        }
        data = new short[SpriteSheetCatalog.SPRITE_COUNT * STRIDE];
        for (int i = 0; i < data.length; i++) data[i] = in.readShort();
        if (in.read() != -1) throw new IOException("Trailing cc.dat data");
        for (int id = 0; id < size(); id++) {
            int i = id * STRIDE, image = data[i + IMAGE];
            int rw = data[i + RENDER_W], rh = data[i + RENDER_H], mode = data[i + RENDER_MODE], gap = data[i + BOTTOM_GAP];
            if (image < 0 || image >= imageNames.length || data[i + WIDTH] <= 0 || data[i + HEIGHT] <= 0
                    || data[i + PIXEL_X] < 0 || data[i + PIXEL_Y] < 0 || rw < 0 || rh < 0 || (rw == 0) != (rh == 0)
                    || data[i + TRIM_X] < 0 || data[i + TRIM_Y] < 0
                    || data[i + PIXEL_X] + rw > images[image * IMAGE_STRIDE + IMAGE_W] || data[i + PIXEL_Y] + rh > images[image * IMAGE_STRIDE + IMAGE_H]
                    || mode < NORMAL || mode > QUARTER || gap < 0 || gap > 1 || (gap != 0 && mode != QUARTER)
                    || data[i + TRIM_X] + rw * (mode >= HALF ? 2 : 1) > data[i + WIDTH]
                    || data[i + TRIM_Y] + rh * (mode == QUARTER ? 2 : 1) > data[i + HEIGHT]) {
                throw new IOException("Invalid sprite geometry: " + id);
            }
        }
    }

    public static synchronized SpriteCatalog load() throws IOException {
        if (instance == null) {
            DataInputStream in = new DataInputStream(ResourceLoader.openRequired(ResourcePaths.SPRITE_CATALOG));
            SpriteCatalog loaded;
            try {
                loaded = new SpriteCatalog(in);
            } finally {
                in.close();
            }
            instance = loaded;
        }
        return instance;
    }

    public int imageCount() { return imageNames.length; }
    String imagePath(int id) { return "/sp/" + imageNames[id]; }
    int variantBase(int id) { return images[id * IMAGE_STRIDE + IMAGE_BASE]; }
    int variantPalette(int id) { return images[id * IMAGE_STRIDE + IMAGE_PALETTE]; }
    int imageWidth(int id) { return images[id * IMAGE_STRIDE + IMAGE_W]; }
    int imageHeight(int id) { return images[id * IMAGE_STRIDE + IMAGE_H]; }
    public int size() { return data.length / STRIDE; }
    public boolean hasSpriteMeta(int id) { return id >= 0 && id < size(); }
    public int getImgIndex(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + IMAGE] : -1; }
    public int getW(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + WIDTH] : 0; }
    public int getH(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + HEIGHT] : 0; }
    public int getAx(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + ANCHOR_X] : 0; }
    public int getAy(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + ANCHOR_Y] : 0; }

    int pixelX(int id) { return data[id * STRIDE + PIXEL_X]; }
    int pixelY(int id) { return data[id * STRIDE + PIXEL_Y]; }
    int renderW(int id) { return data[id * STRIDE + RENDER_W]; }
    int renderH(int id) { return data[id * STRIDE + RENDER_H]; }
    int trimX(int id) { return data[id * STRIDE + TRIM_X]; }
    int trimY(int id) { return data[id * STRIDE + TRIM_Y]; }
    int renderMode(int id) { return data[id * STRIDE + RENDER_MODE]; }
    int bottomGap(int id) { return data[id * STRIDE + BOTTOM_GAP]; }
}
