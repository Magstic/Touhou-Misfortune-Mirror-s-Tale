package touhou;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Image;

public final class ResourceLoader {
    private ResourceLoader() {
    }

    public static InputStream openOptional(String path) {
        ResourcePaths.requireCanonical(path);
        return ResourceLoader.class.getResourceAsStream(path);
    }

    public static InputStream openRequired(String path) throws IOException {
        InputStream in = openOptional(path);
        if (in == null) {
            throw new IOException("Missing resource: " + path);
        }
        return in;
    }

    public static boolean exists(String path) {
        InputStream in = openOptional(path);
        if (in == null) {
            return false;
        }
        closeQuietly(in);
        return true;
    }

    public static byte[] readOptionalBytes(String path) {
        InputStream in = openOptional(path);
        if (in == null) {
            return null;
        }
        try {
            return readAll(in);
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    public static byte[] readRequiredBytes(String path) throws IOException {
        InputStream in = openRequired(path);
        try {
            return readAll(in);
        } finally {
            closeQuietly(in);
        }
    }

    public static Image loadOptionalImage(String path) {
        ResourcePaths.requireCanonical(path);
        try {
            return Image.createImage(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readAll(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int count = in.read(buffer);
            if (count < 0) {
                break;
            }
            if (count > 0) {
                out.write(buffer, 0, count);
            }
        }
        return out.toByteArray();
    }

    public static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ignore) {
        }
    }
}
