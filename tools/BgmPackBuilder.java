import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import touhou.BgmCatalog;

public final class BgmPackBuilder {
    private BgmPackBuilder() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: BgmPackBuilder <res directory> <output snd directory>");
        }
        File sourceDir = new File(args[0]);
        File outputDir = new File(args[1]);
        build(new File(sourceDir, "bgm"), new File(outputDir, "bgm.dat"), false);
        build(new File(sourceDir, "bgm-lite"), new File(outputDir, "bgm-lite.dat"), true);
    }

    static void build(File sourceDir, File outputFile, boolean lite) throws IOException {
        validateSourceEntries(sourceDir, lite);

        int entryCount = BgmCatalog.TRACK_COUNT * BgmCatalog.PART_COUNT;
        byte[][] entries = new byte[entryCount][];
        for (int track = 0; track < BgmCatalog.TRACK_COUNT; track++) {
            if (lite && !BgmCatalog.isBattleTrack(track)) continue;
            File split = new File(sourceDir, String.valueOf(track));
            File single = new File(sourceDir, track + ".mid");
            int base = track * BgmCatalog.PART_COUNT;
            if (split.isDirectory()) {
                validateSplitDirectory(split);
                entries[base + BgmCatalog.PART_INTRO] = readMidi(new File(split, "intro.mid"));
                entries[base + BgmCatalog.PART_LOOP] = readMidi(new File(split, "loop.mid"));
            } else if (single.isFile()) {
                entries[base + BgmCatalog.PART_SINGLE] = readMidi(single);
            } else {
                throw new IOException("Missing BGM track " + track);
            }
        }

        int headerSize = BgmCatalog.HEADER_SIZE + entryCount * BgmCatalog.ENTRY_SIZE;
        int[] offsets = new int[entryCount];
        int[] lengths = new int[entryCount];
        Arrays.fill(offsets, -1);
        int position = headerSize;
        for (int i = 0; i < entryCount; i++) {
            if (entries[i] == null) {
                continue;
            }
            offsets[i] = position;
            lengths[i] = entries[i].length;
            position += entries[i].length;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(position);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(BgmCatalog.PACK_MAGIC);
        out.writeShort(BgmCatalog.PACK_VERSION);
        out.writeByte(BgmCatalog.CODEC_MIDI);
        out.writeByte(BgmCatalog.PART_COUNT);
        out.writeShort(BgmCatalog.TRACK_COUNT);
        out.writeShort(entryCount);
        out.writeInt(0);
        for (int i = 0; i < entryCount; i++) {
            out.writeInt(offsets[i]);
        }
        for (int i = 0; i < entryCount; i++) {
            out.writeInt(lengths[i]);
        }
        for (int i = 0; i < entryCount; i++) {
            if (entries[i] != null) {
                out.write(entries[i]);
            }
        }
        out.flush();

        byte[] pack = bytes.toByteArray();
        verifyPack(pack, entries);
        write(outputFile, pack);
        System.out.println("BgmPackBuilder: wrote " + outputFile.getPath()
                + " tracks=" + BgmCatalog.TRACK_COUNT + " bytes=" + pack.length);
    }

    private static void validateSourceEntries(File sourceDir, boolean lite) throws IOException {
        if (!sourceDir.isDirectory()) {
            throw new IOException("BGM source directory not found: " + sourceDir.getPath());
        }
        File[] children = sourceDir.listFiles();
        if (children == null) {
            throw new IOException("Cannot list BGM source directory: " + sourceDir.getPath());
        }
        boolean[] found = new boolean[BgmCatalog.TRACK_COUNT];
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String name = child.getName();
            int track;
            if (child.isDirectory()) {
                track = parseTrackId(name);
            } else if (child.isFile() && name.endsWith(".mid")) {
                track = parseTrackId(name.substring(0, name.length() - 4));
            } else {
                throw new IOException("Unexpected BGM source entry: " + child.getPath());
            }
            if (!BgmCatalog.isTrack(track) || (lite && !BgmCatalog.isBattleTrack(track)) || found[track]) {
                throw new IOException("Invalid or duplicate BGM track: " + name);
            }
            found[track] = true;
        }
        for (int track = 0; track < found.length; track++) {
            if (!found[track] && (!lite || BgmCatalog.isBattleTrack(track))) {
                throw new IOException("Missing BGM track " + track);
            }
        }
    }

    private static int parseTrackId(String value) throws IOException {
        try {
            if (value.length() == 0 || (value.length() > 1 && value.charAt(0) == '0')) {
                throw new NumberFormatException();
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid BGM track name: " + value);
        }
    }

    private static void validateSplitDirectory(File directory) throws IOException {
        File[] children = directory.listFiles();
        if (children == null || children.length != 2) {
            throw new IOException("BGM split track must contain only intro.mid and loop.mid: " + directory.getPath());
        }
        File intro = new File(directory, "intro.mid");
        File loop = new File(directory, "loop.mid");
        if (!intro.isFile() || !loop.isFile()) {
            throw new IOException("Invalid split BGM track: " + directory.getPath());
        }
    }

    private static byte[] readMidi(File file) throws IOException {
        byte[] bytes = read(file);
        if (bytes.length < 14 || readI32(bytes, 0) != 0x4D546864 || readI32(bytes, 4) != 6) {
            throw new IOException("Invalid MIDI header: " + file.getPath());
        }
        int format = readU16(bytes, 8);
        int midiTracks = readU16(bytes, 10);
        int division = readU16(bytes, 12);
        if ((format != 0 && format != 1) || midiTracks == 0 || division == 0) {
            throw new IOException("Unsupported MIDI header: " + file.getPath());
        }
        return bytes;
    }

    private static void verifyPack(byte[] pack, byte[][] sourceEntries) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(pack));
        int entryCount = BgmCatalog.TRACK_COUNT * BgmCatalog.PART_COUNT;
        if (in.readInt() != BgmCatalog.PACK_MAGIC
                || (in.readUnsignedShort() != BgmCatalog.PACK_VERSION)
                || in.readUnsignedByte() != BgmCatalog.CODEC_MIDI
                || in.readUnsignedByte() != BgmCatalog.PART_COUNT
                || in.readUnsignedShort() != BgmCatalog.TRACK_COUNT
                || in.readUnsignedShort() != entryCount
                || in.readInt() != 0) {
            throw new IOException("Generated BGM pack header is invalid");
        }
        int[] offsets = new int[entryCount];
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = in.readInt();
        }
        for (int i = 0; i < entryCount; i++) {
            int length = in.readInt();
            int expectedLength = sourceEntries[i] == null ? 0 : sourceEntries[i].length;
            if (length != expectedLength || (length == 0 ? offsets[i] != -1 : offsets[i] < 0)) {
                throw new IOException("Generated BGM pack entry is invalid: " + i);
            }
            if (length > 0 && (offsets[i] > pack.length - length)) {
                throw new IOException("Generated BGM pack entry exceeds file: " + i);
            }
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

    private static int readU16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private static int readI32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }
}
