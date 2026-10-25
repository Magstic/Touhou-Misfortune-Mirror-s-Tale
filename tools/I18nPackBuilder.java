import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import touhou.i18n.I18nSchema;
import touhou.i18n.TextId;

public final class I18nPackBuilder {
    private static final int FORMAT_MAGIC = 0x4931384E; // 'I18N'
    private static final int FORMAT_VERSION = 1;
    private static final String[] DEFAULT_LANGUAGES = new String[] { "en", "ja", "zht", "zhs" };

    private I18nPackBuilder() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("Usage: I18nPackBuilder <project-root> <output-dir> [languages...]");
        }
        File root = new File(args[0]);
        File outputDir = new File(args[1]);
        String[] languages;
        if (args.length == 2) {
            languages = DEFAULT_LANGUAGES;
        } else {
            languages = new String[args.length - 2];
            System.arraycopy(args, 2, languages, 0, languages.length);
        }
        build(root, outputDir, languages);
    }

    static void build(File root, File outputDir, String[] languages) throws Exception {
        validateTextIds();
        if (languages == null || languages.length == 0) {
            throw new IOException("No languages configured");
        }

        LanguageSource[] sources = new LanguageSource[languages.length];
        for (int i = 0; i < languages.length; i++) {
            validateLanguageCode(languages[i]);
            sources[i] = readLanguage(root, languages[i]);
        }
        validateMatchingFileSets(sources);
        validateTextSchemas(sources);

        for (int i = 0; i < sources.length; i++) {
            buildOne(root, outputDir, sources[i]);
        }
    }

    private static void validateTextIds() throws Exception {
        boolean[] used = new boolean[TextId.COUNT];
        Field[] fields = TextId.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.getType() != Integer.TYPE || !Modifier.isStatic(field.getModifiers()) || "COUNT".equals(field.getName())) {
                continue;
            }
            int value = field.getInt(null);
            if (value < 0 || value >= TextId.COUNT) {
                throw new IOException("TextId out of range: " + field.getName() + "=" + value);
            }
            if (used[value]) {
                throw new IOException("Duplicate TextId value: " + field.getName() + "=" + value);
            }
            used[value] = true;
        }
    }

    private static LanguageSource readLanguage(File root, String language) throws IOException {
        File directory = new File(new File(new File(root, "res"), "i18n"), language);
        if (!directory.isDirectory()) {
            throw new IOException("Missing i18n directory: " + directory.getPath());
        }
        List<File> files = new ArrayList<File> ();
        collectFiles(directory, files);
        if (files.size() == 0) {
            throw new IOException("Empty i18n directory: " + directory.getPath());
        }

        String[] paths = new String[files.size()];
        byte[][] bytes = new byte[files.size()][];
        String[][] lines = new String[files.size()][];
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            paths[i] = relativePath(directory, file);
            bytes[i] = read(file);
            String lower = file.getName().toLowerCase();
            if (!lower.endsWith(".txt") && !lower.endsWith(".dat")) {
                throw new IOException("Unsupported localized resource: " + file.getPath());
            }
            lines[i] = splitLines(decodeUtf8(bytes[i], file));
        }
        return new LanguageSource(language, directory, paths, bytes, lines);
    }

    private static void validateMatchingFileSets(LanguageSource[] sources) throws IOException {
        String[] expected = sources[0].paths;
        for (int i = 1; i < sources.length; i++) {
            if (!Arrays.equals(expected, sources[i].paths)) {
                throw new IOException("Localized file set differs: " + sources[0].language + " vs " + sources[i].language);
            }
        }
    }

    private static void validateTextSchemas(LanguageSource[] sources) throws IOException {
        int uiIndex = requireEntry(sources[0], "ui.txt");
        int spellNameIndex = requireEntry(sources[0], "spcard.dat");
        int spellCommentIndex = requireEntry(sources[0], "sprac.dat");

        for (int language = 0; language < sources.length; language++) {
            LanguageSource source = sources[language];
            requireLineCount(source, uiIndex, I18nSchema.UI_LINE_COUNT);
            requireLineCount(source, spellNameIndex, I18nSchema.SPELL_LINE_COUNT);
            requireLineCount(source, spellCommentIndex, I18nSchema.SPELL_LINE_COUNT);
        }

        for (int file = 0; file < sources[0].paths.length; file++) {
            int lineCount = sources[0].lines[file].length;
            for (int language = 1; language < sources.length; language++) {
                if (sources[language].lines[file].length != lineCount) {
                    throw new IOException("Line count differs for " + sources[0].paths[file] + ": "
                            + sources[0].language + "=" + lineCount + " " + sources[language].language + "="
                            + sources[language].lines[file].length);
                }
            }
        }

        validateBlankMask(sources, spellNameIndex);
        validateBlankMask(sources, spellCommentIndex);
    }

    private static void validateBlankMask(LanguageSource[] sources, int fileIndex) throws IOException {
        String path = sources[0].paths[fileIndex];
        String[] expected = sources[0].lines[fileIndex];
        for (int language = 1; language < sources.length; language++) {
            String[] actual = sources[language].lines[fileIndex];
            for (int line = 0; line < expected.length; line++) {
                if ((expected[line].length() == 0) != (actual[line].length() == 0)) {
                    throw new IOException("Missing localized value: " + path + " line=" + (line + 1)
                            + " languages=" + sources[0].language + "," + sources[language].language);
                }
            }
        }
    }

    private static void requireLineCount(LanguageSource source, int index, int expected) throws IOException {
        int actual = source.lines[index].length;
        if (actual != expected) {
            throw new IOException(source.language + "/" + source.paths[index] + " lines=" + actual + " expected=" + expected);
        }
    }

    private static int requireEntry(LanguageSource source, String path) throws IOException {
        int index = Arrays.binarySearch(source.paths, path);
        if (index < 0) {
            throw new IOException("Missing localized resource: " + source.language + "/" + path);
        }
        return index;
    }

    private static void buildOne(File root, File outputDir, LanguageSource source) throws Exception {
        byte[] font = buildFont(root, source.language, source.directory);
        int sourceCount = source.paths.length;
        int entryCount = sourceCount + 1;
        String[] relativePaths = new String[entryCount];
        byte[][] data = new byte[entryCount][];
        System.arraycopy(source.paths, 0, relativePaths, 0, sourceCount);
        System.arraycopy(source.bytes, 0, data, 0, sourceCount);
        relativePaths[sourceCount] = "font/font.dat";
        data[sourceCount] = font;
        sortEntries(relativePaths, data);

        byte[][] keys = new byte[entryCount][];
        int tableSize = 8;
        for (int i = 0; i < entryCount; i++) {
            keys[i] = ("/i18n/" + source.language + "/" + relativePaths[i]).getBytes("UTF-8");
            tableSize += 2 + keys[i].length + 8;
        }

        int[] offsets = new int[entryCount];
        int position = tableSize;
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = position;
            position += data[i].length;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(position);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(FORMAT_MAGIC);
        out.writeByte(FORMAT_VERSION);
        out.writeByte(0);
        out.writeShort(entryCount);
        for (int i = 0; i < entryCount; i++) {
            out.writeShort(keys[i].length);
            out.write(keys[i]);
            out.writeInt(offsets[i]);
            out.writeInt(data[i].length);
        }
        for (int i = 0; i < entryCount; i++) {
            out.write(data[i]);
        }
        out.flush();

        byte[] pack = bytes.toByteArray();
        verifyPack(pack, source.language, relativePaths, data);
        File output = new File(outputDir, source.language + ".dat");
        write(output, pack);
        System.out.println("I18nPackBuilder: wrote " + output.getPath() + " entries=" + entryCount + " bytes=" + pack.length);
    }

    private static byte[] buildFont(File root, String language, File scanDirectory) throws Exception {
        String fontName = "ja".equals(language) ? "ja.ttf" : "zhs".equals(language) ? "zh_hans.ttf" : "zh_hant.ttf";
        File fontFile = new File(new File(root, "font"), fontName);
        File generated = new File(new File(new File(root, "build"), "i18n-font"), language + "-font.dat");
        FontDatBuilder.BuildConfig config = new FontDatBuilder.BuildConfig();
        config.ttfFile = fontFile;
        config.cellW = 12;
        config.cellH = 12;
        config.baseline = Integer.valueOf(10);
        config.fontSize = 12;
        config.outFile = generated;
        config.scanDirectory = scanDirectory;
        FontDatBuilder.BuildResult result = FontDatBuilder.buildToFile(config);
        if (result.missingGlyphCount != 0) {
            throw new IOException("Font is missing localized glyphs for " + language + "\n" + result.missingGlyphReport);
        }
        return read(generated);
    }

    private static void verifyPack(byte[] pack, String language, String[] paths, byte[][] expectedData) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(pack));
        if (in.readInt() != 0x4931384E || in.readUnsignedByte() != 1 || in.readUnsignedByte() != 0
                || in.readUnsignedShort() != paths.length) {
            throw new IOException("Generated i18n pack header is invalid: " + language);
        }
        int[] offsets = new int[paths.length];
        int[] lengths = new int[paths.length];
        for (int i = 0; i < paths.length; i++) {
            int keyLength = in.readUnsignedShort();
            byte[] keyBytes = new byte[keyLength];
            in.readFully(keyBytes);
            String key = decodeUtf8(keyBytes, new File(language + ".dat#key" + i));
            String expectedKey = "/i18n/" + language + "/" + paths[i];
            if (!expectedKey.equals(key)) {
                throw new IOException("Generated i18n pack key mismatch: " + key);
            }
            offsets[i] = in.readInt();
            lengths[i] = in.readInt();
            if (lengths[i] != expectedData[i].length || offsets[i] > pack.length - lengths[i]) {
                throw new IOException("Generated i18n pack entry is invalid: " + key);
            }
        }
        for (int i = 0; i < paths.length; i++) {
            for (int b = 0; b < lengths[i]; b++) {
                if (pack[offsets[i] + b] != expectedData[i][b]) {
                    throw new IOException("Generated i18n pack payload differs: " + paths[i]);
                }
            }
        }
    }

    private static void collectFiles(File directory, List<File> output) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            throw new IOException("Cannot list i18n directory: " + directory.getPath());
        }
        Arrays.sort(children);
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                if ("font".equals(child.getName())) {
                    throw new IOException("Generated font directory must not be stored in source resources: " + child.getPath());
                }
                collectFiles(child, output);
            } else if (child.isFile()) {
                output.add(child);
            } else {
                throw new IOException("Unsupported i18n entry: " + child.getPath());
            }
        }
    }

    private static String relativePath(File root, File file) throws IOException {
        String rootPath = root.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(rootPath + File.separator)) {
            throw new IOException("Localized file is outside language directory: " + file.getPath());
        }
        return filePath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
    }

    private static String decodeUtf8(byte[] bytes, File file) throws IOException {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            throw new IOException("UTF-8 BOM is not allowed: " + file.getPath());
        }
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IOException("Invalid UTF-8: " + file.getPath());
        }
    }

    private static String[] splitLines(String text) {
        int length = text.length();
        while (length > 0 && (text.charAt(length - 1) == '\r' || text.charAt(length - 1) == '\n')) {
            length--;
        }
        if (length == 0) {
            return new String[0];
        }
        List<String> lines = new ArrayList<String> ();
        int start = 0;
        for (int i = 0; i <= length; i++) {
            if (i < length && text.charAt(i) != '\n') {
                continue;
            }
            int end = i;
            if (end > start && text.charAt(end - 1) == '\r') {
                end--;
            }
            lines.add(text.substring(start, end));
            start = i + 1;
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static void sortEntries(String[] paths, byte[][] bytes) {
        for (int i = 1; i < paths.length; i++) {
            String path = paths[i];
            byte[] data = bytes[i];
            int j = i - 1;
            while (j >= 0 && paths[j].compareTo(path) > 0) {
                paths[j + 1] = paths[j];
                bytes[j + 1] = bytes[j];
                j--;
            }
            paths[j + 1] = path;
            bytes[j + 1] = data;
        }
    }

    private static byte[] read(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
            byte[] buffer = new byte[8192];
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
        } finally {
            in.close();
        }
    }

    private static void write(File file, byte[] bytes) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create output directory: " + parent.getPath());
        }
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    private static void validateLanguageCode(String language) {
        if (language == null || language.length() == 0 || language.indexOf('/') >= 0 || language.indexOf('\\') >= 0
                || language.indexOf("..") >= 0) {
            throw new IllegalArgumentException("Invalid language code: " + language);
        }
    }

    private static final class LanguageSource {
        final String language;
        final File directory;
        final String[] paths;
        final byte[][] bytes;
        final String[][] lines;

        LanguageSource(String language, File directory, String[] paths, byte[][] bytes, String[][] lines) {
            this.language = language;
            this.directory = directory;
            this.paths = paths;
            this.bytes = bytes;
            this.lines = lines;
        }
    }
}
