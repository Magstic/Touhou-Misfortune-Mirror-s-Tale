package touhou.replay;

import javax.microedition.rms.RecordStore;

final class ReplayRmsStore {
    private static final String STORE = "th_replay";
    private static final int DATA_SEGMENT_SIZE = 1024;
    private static final int DATA_SEGMENT_COUNT = ReplayCodec.ENCODED_SIZE / DATA_SEGMENT_SIZE;
    private static final int FULL_DATA_OFFSET = 2;
    private static final int BOSS_DATA_OFFSET = FULL_DATA_OFFSET + DATA_SEGMENT_COUNT;
    private static final int RECORDS_PER_SLOT = 2 + DATA_SEGMENT_COUNT + DATA_SEGMENT_COUNT;

    private ReplayRmsStore() {
    }

    static boolean warmup() {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    static boolean invalidateSlot(int slot) {
        if (!isValidSlot(slot)) {
            return false;
        }
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            byte[] emptyHeader = new byte[ReplayHeader.SIZE];
            store.setRecord(baseRecordId(slot), emptyHeader, 0, emptyHeader.length);
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    static boolean saveSlot(int slot, ReplayHeader header, byte[] name, byte[] fullInput, byte[] bossInput) {
        if (!isValidSlot(slot) || header == null || !header.hasCurrentFormat()) {
            return false;
        }
        if (name == null || name.length != ReplayFormat.NAME_SIZE
                || fullInput == null || fullInput.length != ReplayCodec.ENCODED_SIZE
                || (bossInput != null && bossInput.length != ReplayCodec.ENCODED_SIZE)) {
            return false;
        }

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            int base = baseRecordId(slot);

            byte[] emptyHeader = new byte[ReplayHeader.SIZE];
            store.setRecord(base, emptyHeader, 0, emptyHeader.length);
            store.setRecord(base + 1, name, 0, name.length);
            writeSegments(store, base + FULL_DATA_OFFSET, fullInput);
            if (bossInput != null) {
                writeSegments(store, base + BOSS_DATA_OFFSET, bossInput);
            } else {
                writeEmptySegments(store, base + BOSS_DATA_OFFSET);
            }

            byte[] headerBytes = header.bytes();
            store.setRecord(base, headerBytes, 0, ReplayHeader.SIZE);
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    static boolean loadSlot(int slot, ReplayHeader header, byte[] name, byte[] fullInput, byte[] bossInput) {
        if (!isValidSlot(slot) || header == null || name == null || fullInput == null || bossInput == null) {
            return false;
        }
        if (name.length != ReplayFormat.NAME_SIZE
                || fullInput.length != ReplayCodec.ENCODED_SIZE
                || bossInput.length != ReplayCodec.ENCODED_SIZE) {
            return false;
        }

        clearOutputs(header, name, fullInput, bossInput);
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            int base = baseRecordId(slot);

            byte[] headerBytes = store.getRecord(base);
            if (headerBytes == null || headerBytes.length != ReplayHeader.SIZE) {
                return false;
            }
            header.load(headerBytes);
            if (!header.hasCurrentFormat() || header.getFlag() == ReplayHeader.FLAG_NONE) {
                clearOutputs(header, name, fullInput, bossInput);
                return false;
            }

            byte[] storedName = store.getRecord(base + 1);
            if (storedName == null || storedName.length != ReplayFormat.NAME_SIZE) {
                clearOutputs(header, name, fullInput, bossInput);
                return false;
            }
            System.arraycopy(storedName, 0, name, 0, name.length);
            if (!readSegments(store, base + FULL_DATA_OFFSET, fullInput)
                    || !readSegments(store, base + BOSS_DATA_OFFSET, bossInput)) {
                clearOutputs(header, name, fullInput, bossInput);
                return false;
            }
            return true;
        } catch (Throwable error) {
            clearOutputs(header, name, fullInput, bossInput);
            return false;
        } finally {
            close(store);
        }
    }

    static boolean loadAllSlotHeadersAndNames(ReplayHeader[] headers, byte[][] names) {
        if (headers == null || names == null
                || headers.length != ReplayFormat.SLOT_COUNT || names.length != ReplayFormat.SLOT_COUNT) {
            return false;
        }
        clearSummaries(headers, names);

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
                ReplayHeader header = headers[slot];
                byte[] name = names[slot];
                if (header == null || name == null || name.length != ReplayFormat.NAME_SIZE) {
                    continue;
                }
                int base = baseRecordId(slot);
                byte[] headerBytes = store.getRecord(base);
                byte[] nameBytes = store.getRecord(base + 1);
                if (headerBytes == null || headerBytes.length != ReplayHeader.SIZE
                        || nameBytes == null || nameBytes.length != ReplayFormat.NAME_SIZE) {
                    continue;
                }
                header.load(headerBytes);
                if (!header.hasCurrentFormat() || header.getFlag() == ReplayHeader.FLAG_NONE) {
                    header.clear();
                    continue;
                }
                System.arraycopy(nameBytes, 0, name, 0, name.length);
            }
            return true;
        } catch (Throwable error) {
            clearSummaries(headers, names);
            return false;
        } finally {
            close(store);
        }
    }

    static boolean loadSlotHeaderAndName(int slot, ReplayHeader header, byte[] name) {
        if (!isValidSlot(slot) || header == null || name == null || name.length != ReplayFormat.NAME_SIZE) {
            return false;
        }
        header.clear();
        clear(name);

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            int base = baseRecordId(slot);
            byte[] headerBytes = store.getRecord(base);
            byte[] nameBytes = store.getRecord(base + 1);
            if (headerBytes == null || headerBytes.length != ReplayHeader.SIZE
                    || nameBytes == null || nameBytes.length != ReplayFormat.NAME_SIZE) {
                return false;
            }
            header.load(headerBytes);
            if (!header.hasCurrentFormat() || header.getFlag() == ReplayHeader.FLAG_NONE) {
                header.clear();
                return false;
            }
            System.arraycopy(nameBytes, 0, name, 0, name.length);
            return true;
        } catch (Throwable error) {
            header.clear();
            clear(name);
            return false;
        } finally {
            close(store);
        }
    }

    static boolean clearSlot(int slot) {
        if (!isValidSlot(slot)) {
            return false;
        }
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE, true);
            ensureRecordCount(store);
            int base = baseRecordId(slot);
            byte[] emptyHeader = new byte[ReplayHeader.SIZE];
            byte[] emptyName = new byte[ReplayFormat.NAME_SIZE];
            byte[] emptySegment = new byte[DATA_SEGMENT_SIZE];
            store.setRecord(base, emptyHeader, 0, emptyHeader.length);
            store.setRecord(base + 1, emptyName, 0, emptyName.length);
            for (int segment = 0; segment < DATA_SEGMENT_COUNT; segment++) {
                store.setRecord(base + FULL_DATA_OFFSET + segment, emptySegment, 0, emptySegment.length);
                store.setRecord(base + BOSS_DATA_OFFSET + segment, emptySegment, 0, emptySegment.length);
            }
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    private static void writeSegments(RecordStore store, int firstRecordId, byte[] data) throws Exception {
        int offset = 0;
        for (int segment = 0; segment < DATA_SEGMENT_COUNT; segment++) {
            store.setRecord(firstRecordId + segment, data, offset, DATA_SEGMENT_SIZE);
            offset += DATA_SEGMENT_SIZE;
        }
    }

    private static void writeEmptySegments(RecordStore store, int firstRecordId) throws Exception {
        byte[] empty = new byte[DATA_SEGMENT_SIZE];
        for (int segment = 0; segment < DATA_SEGMENT_COUNT; segment++) {
            store.setRecord(firstRecordId + segment, empty, 0, empty.length);
        }
    }

    private static boolean readSegments(RecordStore store, int firstRecordId, byte[] output) throws Exception {
        int offset = 0;
        for (int segment = 0; segment < DATA_SEGMENT_COUNT; segment++) {
            byte[] data = store.getRecord(firstRecordId + segment);
            if (data == null || data.length != DATA_SEGMENT_SIZE) {
                return false;
            }
            System.arraycopy(data, 0, output, offset, DATA_SEGMENT_SIZE);
            offset += DATA_SEGMENT_SIZE;
        }
        return true;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < ReplayFormat.SLOT_COUNT;
    }

    private static int baseRecordId(int slot) {
        return slot * RECORDS_PER_SLOT + 1;
    }

    private static void ensureRecordCount(RecordStore store) throws Exception {
        int needed = ReplayFormat.SLOT_COUNT * RECORDS_PER_SLOT;
        int count = store.getNumRecords();
        while (count < needed) {
            int slotOffset = count % RECORDS_PER_SLOT;
            byte[] empty;
            if (slotOffset == 0) {
                empty = new byte[ReplayHeader.SIZE];
            } else if (slotOffset == 1) {
                empty = new byte[ReplayFormat.NAME_SIZE];
            } else {
                empty = new byte[DATA_SEGMENT_SIZE];
            }
            store.addRecord(empty, 0, empty.length);
            count++;
        }
    }

    private static void clearOutputs(ReplayHeader header, byte[] name, byte[] fullInput, byte[] bossInput) {
        header.clear();
        clear(name);
        clear(fullInput);
        clear(bossInput);
    }

    private static void clearSummaries(ReplayHeader[] headers, byte[][] names) {
        for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
            if (headers[slot] != null) {
                headers[slot].clear();
            }
            if (names[slot] != null) {
                clear(names[slot]);
            }
        }
    }

    private static void clear(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }

    private static void close(RecordStore store) {
        if (store != null) {
            try {
                store.closeRecordStore();
            } catch (Throwable ignored) {
            }
        }
    }
}
