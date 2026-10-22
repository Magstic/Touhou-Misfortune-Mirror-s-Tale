package touhou.i18n;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import touhou.ResourceLoader;
import touhou.ResourcePaths;

final class I18nPack {
    private static final int FORMAT_VERSION = 1;
    private static final int HEADER_SIZE = 8;
    private static byte[] pack;
    private static String[] keys;
    private static int[] offsets;
    private static int[] lengths;
    private static int[] slotToEntry;
    private static int slotMask;

    private I18nPack() {
    }

    static void loadForCurrentLanguage() throws IOException {
        String path = ResourcePaths.languagePack(I18n.getLanguageCode());
        byte[] bytes = ResourceLoader.readRequiredBytes(path);
        if (!parse(bytes)) {
            clear();
            throw new IOException("Invalid i18n pack: " + path);
        }
    }

    static byte[] readBytes(String path) {
        if (path == null || pack == null || slotToEntry == null) {
            return null;
        }
        int index = findEntry(path);
        if (index < 0) {
            return null;
        }
        int offset = offsets[index];
        int length = lengths[index];
        byte[] out = new byte[length];
        System.arraycopy(pack, offset, out, 0, length);
        return out;
    }

    private static int findEntry(String key) {
        int position = mix(key.hashCode()) & slotMask;
        for (int i = 0; i < slotToEntry.length; i++) {
            int value = slotToEntry[position];
            if (value == 0) {
                return -1;
            }
            int entryIndex = value - 1;
            if (keys[entryIndex].equals(key)) {
                return entryIndex;
            }
            position = (position + 1) & slotMask;
        }
        return -1;
    }

    private static int mix(int hash) {
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        return hash ^ (hash >>> 16);
    }

    private static void clear() {
        pack = null;
        keys = null;
        offsets = null;
        lengths = null;
        slotToEntry = null;
        slotMask = 0;
    }

    private static boolean parse(byte[] bytes) {
        if (bytes == null || bytes.length < HEADER_SIZE) {
            return false;
        }
        int position = 0;
        if (bytes[position++] != 'I' || bytes[position++] != '1' || bytes[position++] != '8' || bytes[position++] != 'N') {
            return false;
        }
        if ((bytes[position++] & 0xFF) != FORMAT_VERSION || bytes[position++] != 0) {
            return false;
        }
        int count = readU16(bytes, position);
        position += 2;
        if (count <= 0) {
            return false;
        }

        String[] parsedKeys = new String[count];
        int[] parsedOffsets = new int[count];
        int[] parsedLengths = new int[count];
        String expectedPrefix = "/i18n/" + I18n.getLanguageCode() + "/";
        String previousKey = null;
        for (int i = 0; i < count; i++) {
            if (position + 2 > bytes.length) {
                return false;
            }
            int keyLength = readU16(bytes, position);
            position += 2;
            if (keyLength <= 0 || position + keyLength + 8 > bytes.length) {
                return false;
            }
            try {
                parsedKeys[i] = new String(bytes, position, keyLength, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return false;
            }
            if (!parsedKeys[i].startsWith(expectedPrefix) || parsedKeys[i].length() == expectedPrefix.length()
                    || (previousKey != null && previousKey.compareTo(parsedKeys[i]) >= 0)) {
                return false;
            }
            previousKey = parsedKeys[i];
            position += keyLength;
            int offset = readI32(bytes, position);
            position += 4;
            int length = readI32(bytes, position);
            position += 4;
            if (offset < 0 || length < 0 || offset > bytes.length - length) {
                return false;
            }
            parsedOffsets[i] = offset;
            parsedLengths[i] = length;
        }

        int dataPosition = position;
        for (int i = 0; i < count; i++) {
            if (parsedOffsets[i] != dataPosition) {
                return false;
            }
            dataPosition += parsedLengths[i];
        }
        if (dataPosition != bytes.length) {
            return false;
        }

        int capacity = 16;
        while (capacity < count * 2) {
            capacity <<= 1;
        }
        int[] slots = new int[capacity];
        int mask = capacity - 1;
        for (int i = 0; i < count; i++) {
            int slot = mix(parsedKeys[i].hashCode()) & mask;
            while (slots[slot] != 0) {
                if (parsedKeys[slots[slot] - 1].equals(parsedKeys[i])) {
                    return false;
                }
                slot = (slot + 1) & mask;
            }
            slots[slot] = i + 1;
        }

        pack = bytes;
        keys = parsedKeys;
        offsets = parsedOffsets;
        lengths = parsedLengths;
        slotToEntry = slots;
        slotMask = mask;
        return true;
    }

    private static int readU16(byte[] bytes, int position) {
        return ((bytes[position] & 0xFF) << 8) | (bytes[position + 1] & 0xFF);
    }

    private static int readI32(byte[] bytes, int position) {
        return ((bytes[position] & 0xFF) << 24) | ((bytes[position + 1] & 0xFF) << 16)
                | ((bytes[position + 2] & 0xFF) << 8) | (bytes[position + 3] & 0xFF);
    }
}
