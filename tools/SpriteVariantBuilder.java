import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Prepare the fixed-opacity sheets used by StageAssets without indexed PNG transparency. */
public final class SpriteVariantBuilder {
    private static final int[] HALF_OPACITY_IMAGES = {
        15, 16, 19, 20, 21, 22, 26, 29, 30, 31, 32, 35, 36,
        54, 55, 56, 67, 79, 88, 91, 99, 105, 113
    };
    private static final int[] BACKGROUND_IMAGES = { 59, 72, 73, 83, 98, 107 };
    private static final int MAX_BACKGROUND_DARKNESS = 2;

    private SpriteVariantBuilder() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File sourceDir = new File(args[0], "res/sp");
        File outputDir = new File(args[1]);
        Files.createDirectories(outputDir.toPath());
        int count = 0;
        for (int i = 0; i < HALF_OPACITY_IMAGES.length; i++) {
            build(sourceDir, outputDir, HALF_OPACITY_IMAGES[i], 128, 0);
            count++;
        }
        for (int i = 0; i < BACKGROUND_IMAGES.length; i++) {
            int image = BACKGROUND_IMAGES[i];
            int alpha = image == 83 ? 127 : 128;
            for (int darkness = 0; darkness <= MAX_BACKGROUND_DARKNESS; darkness++) {
                build(sourceDir, outputDir, image, alpha, darkness);
                count++;
            }
        }
        System.out.println("SpriteVariantBuilder: wrote " + count + " fixed-opacity RGBA PNGs");
    }

    private static void build(File sourceDir, File outputDir, int image, int alpha, int darkness) throws Exception {
        String name = String.format(Locale.ROOT, "%03d", image);
        BufferedImage source = ImageIO.read(new File(sourceDir, name + ".png"));
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int colorMask = (255 >>> darkness) * 0x010101;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                int opacity = (pixel >>> 24) * alpha / 255;
                int color = ((pixel & 0xFFFFFF) >>> darkness) & colorMask;
                result.setRGB(x, y, (opacity << 24) | color);
            }
        }
        ImageIO.write(result, "png", new File(outputDir, name + "_a" + alpha + "_d" + darkness + ".png"));
    }
}
