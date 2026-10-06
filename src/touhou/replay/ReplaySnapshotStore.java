package touhou.replay;

import javax.microedition.rms.RecordStore;

final class ReplaySnapshotStore {
    private final String storeName;

    ReplaySnapshotStore(String storeName) {
        this.storeName = storeName;
    }

    boolean warmup() {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(storeName, true);
            ensureRecordCount(store);
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    boolean loadPresence(boolean[] output) {
        if (output == null || output.length != ReplayFormat.SLOT_COUNT) {
            return false;
        }
        clear(output);

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(storeName, true);
            ensureRecordCount(store);
            for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
                byte[] record = store.getRecord(recordId(slot));
                output[slot] = hasValidRecord(record);
            }
            return true;
        } catch (Throwable error) {
            clear(output);
            return false;
        } finally {
            close(store);
        }
    }

    boolean hasSlot(int slot) {
        if (!isValidSlot(slot)) {
            return false;
        }
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(storeName, true);
            ensureRecordCount(store);
            return hasValidRecord(store.getRecord(recordId(slot)));
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    byte[] loadSlot(int slot) {
        if (!isValidSlot(slot)) {
            return null;
        }
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(storeName, true);
            ensureRecordCount(store);
            byte[] record = store.getRecord(recordId(slot));
            if (!hasValidRecord(record)) {
                return null;
            }
            int length = ReplayChecksum.readI32LE(record, 0);
            byte[] output = new byte[length];
            System.arraycopy(record, 4, output, 0, length);
            return output;
        } catch (Throwable error) {
            return null;
        } finally {
            close(store);
        }
    }

    boolean saveSlot(int slot, byte[] data) {
        if (!isValidSlot(slot) || (data != null && (data.length == 0 || data.length > ReplayFormat.SNAPSHOT_MAX_SIZE))) {
            return false;
        }

        int length = (data != null) ? data.length : 0;
        byte[] record = new byte[4 + length];
        ReplayChecksum.writeI32LE(record, 0, length);
        if (length > 0) {
            System.arraycopy(data, 0, record, 4, length);
        }

        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(storeName, true);
            ensureRecordCount(store);
            store.setRecord(recordId(slot), record, 0, record.length);
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            close(store);
        }
    }

    boolean clearSlot(int slot) {
        return saveSlot(slot, null);
    }

    private static boolean hasValidRecord(byte[] record) {
        if (record == null || record.length < 4) {
            return false;
        }
        int length = ReplayChecksum.readI32LE(record, 0);
        return length > 0
                && length <= ReplayFormat.SNAPSHOT_MAX_SIZE
                && record.length == 4 + length;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < ReplayFormat.SLOT_COUNT;
    }

    private static int recordId(int slot) {
        return slot + 1;
    }

    private static void ensureRecordCount(RecordStore store) throws Exception {
        int count = store.getNumRecords();
        while (count < ReplayFormat.SLOT_COUNT) {
            byte[] empty = new byte[4];
            store.addRecord(empty, 0, empty.length);
            count++;
        }
    }

    private static void clear(boolean[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = false;
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
