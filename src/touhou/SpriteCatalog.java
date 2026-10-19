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
    private static SpriteCatalog instance;
    private final String[] imageNames;
    // Per image: variant base (-1 for a file), palette (-1 for a file), width, height.
    private final short[] images;
    private final short[] data;

    private SpriteCatalog(DataInputStream in) throws IOException {
        if (in.readInt() != 0x43433031 || in.readUnsignedShort() != 1) {
            throw new IOException("Invalid cc.dat header");
        }
        int imageCount = in.readUnsignedShort();
        if (imageCount == 0 || imageCount > 32768 || in.readUnsignedShort() != SpriteSheetCatalog.SPRITE_COUNT) {
            throw new IOException("Invalid cc.dat counts");
        }
        imageNames = new String[imageCount];
        images = new short[imageNames.length * 4];
        for (int id = 0; id < imageNames.length; id++) {
            String name = in.readUTF();
            imageNames[id] = name;
            int i = id * 4;
            for (int k = 0; k < 4; k++) images[i + k] = in.readShort();
            int base = images[i], palette = images[i + 1];
            if (images[i + 2] <= 0 || images[i + 3] <= 0) {
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
            } else if (base < 0 || base >= id || images[base * 4] != -1
                    || palette < 0 || palette >= 6 || name.length() != 0
                    || images[i + 2] != images[base * 4 + 2] || images[i + 3] != images[base * 4 + 3]) {
                throw new IOException("Invalid image variant: " + id);
            }
        }
        data = new short[SpriteSheetCatalog.SPRITE_COUNT * STRIDE];
        for (int i = 0; i < data.length; i++) data[i] = in.readShort();
        if (in.read() != -1) throw new IOException("Trailing cc.dat data");
        for (int id = 0; id < size(); id++) {
            int i = id * STRIDE, image = data[i];
            int rw = data[i + 7], rh = data[i + 8], mode = data[i + 11], gap = data[i + 12];
            if (image < 0 || image >= imageNames.length || data[i + 1] <= 0 || data[i + 2] <= 0
                    || data[i + 5] < 0 || data[i + 6] < 0 || rw < 0 || rh < 0 || (rw == 0) != (rh == 0)
                    || data[i + 9] < 0 || data[i + 10] < 0
                    || data[i + 5] + rw > images[image * 4 + 2] || data[i + 6] + rh > images[image * 4 + 3]
                    || mode < NORMAL || mode > QUARTER || gap < 0 || gap > 1 || (gap != 0 && mode != QUARTER)
                    || data[i + 9] + rw * (mode >= HALF ? 2 : 1) > data[i + 1]
                    || data[i + 10] + rh * (mode == QUARTER ? 2 : 1) > data[i + 2]) {
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
    int variantBase(int id) { return images[id * 4]; }
    int variantPalette(int id) { return images[id * 4 + 1]; }
    int imageWidth(int id) { return images[id * 4 + 2]; }
    int imageHeight(int id) { return images[id * 4 + 3]; }
    public int size() { return data.length / STRIDE; }
    public boolean hasSpriteMeta(int id) { return id >= 0 && id < size(); }
    public int getImgIndex(int id) { return hasSpriteMeta(id) ? data[id * STRIDE] : -1; }
    public int getW(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + 1] : 0; }
    public int getH(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + 2] : 0; }
    public int getAx(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + 3] : 0; }
    public int getAy(int id) { return hasSpriteMeta(id) ? data[id * STRIDE + 4] : 0; }

    int pixelX(int id) { return data[id * STRIDE + 5]; }
    int pixelY(int id) { return data[id * STRIDE + 6]; }
    int renderW(int id) { return data[id * STRIDE + 7]; }
    int renderH(int id) { return data[id * STRIDE + 8]; }
    int trimX(int id) { return data[id * STRIDE + 9]; }
    int trimY(int id) { return data[id * STRIDE + 10]; }
    int renderMode(int id) { return data[id * STRIDE + 11]; }
    int bottomGap(int id) { return data[id * STRIDE + 12]; }
}
