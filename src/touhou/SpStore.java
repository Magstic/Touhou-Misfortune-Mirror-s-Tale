package touhou;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

/** Fixed-size scratch-pad storage. A successful write confirms every affected RMS record. */
public final class SpStore {
    private static final String STORE = "th_sp";
    private static final int SEG_SIZE = 1024;
    private static final int SEGMENTS = 32;
    private static final int SIZE = SEG_SIZE * SEGMENTS;

    private static byte[] cache;

    private SpStore() {
    }

    public static synchronized byte[] read(int offset, int length) {
        requireRange(offset, length);
        ensureLoaded();
        byte[] out = new byte[length];
        System.arraycopy(cache, offset, out, 0, length);
        return out;
    }

    public static synchronized void write(int offset, byte[] src) {
        if (src == null) {
            throw new NullPointerException("src");
        }
        requireRange(offset, src.length);
        if (src.length == 0) {
            return;
        }
        ensureLoaded();

        boolean committed = false;
        try {
            RecordStore store = null;
            boolean failed = true;
            try {
                store = RecordStore.openRecordStore(STORE, false);
                requireRecordCount(store);
                byte[] segment = new byte[SEG_SIZE];
                int first = offset / SEG_SIZE;
                int last = (offset + src.length - 1) / SEG_SIZE;
                for (int index = first; index <= last; index++) {
                    int base = index * SEG_SIZE;
                    System.arraycopy(cache, base, segment, 0, SEG_SIZE);
                    int start = (offset > base) ? offset : base;
                    int end = offset + src.length;
                    if (end > base + SEG_SIZE) {
                        end = base + SEG_SIZE;
                    }
                    System.arraycopy(src, start - offset, segment, start - base, end - start);
                    store.setRecord(index + 1, segment, 0, SEG_SIZE);
                }
                failed = false;
            } catch (RecordStoreException e) {
                throw new StorageException("Storage write failed: " + e.toString());
            } finally {
                close(store, failed);
            }
            System.arraycopy(src, 0, cache, offset, src.length);
            committed = true;
        } finally {
            if (!committed) {
                // RMS cannot roll back a multi-record write. Reload the actual disk state, never dirty data.
                cache = null;
            }
        }
    }

    private static void ensureLoaded() {
        if (cache != null) {
            return;
        }
        RecordStore store = null;
        boolean failed = true;
        byte[] loaded = new byte[SIZE];
        try {
            boolean created = false;
            try {
                store = RecordStore.openRecordStore(STORE, false);
            } catch (RecordStoreNotFoundException e) {
                store = RecordStore.openRecordStore(STORE, true);
                created = true;
            }
            if (created) {
                byte[] empty = new byte[SEG_SIZE];
                for (int i = 0; i < SEGMENTS; i++) {
                    store.addRecord(empty, 0, empty.length);
                }
            }
            requireRecordCount(store);
            for (int index = 0; index < SEGMENTS; index++) {
                byte[] record = store.getRecord(index + 1);
                if (record == null || record.length != SEG_SIZE) {
                    throw new StorageException("Invalid storage record: " + (index + 1));
                }
                System.arraycopy(record, 0, loaded, index * SEG_SIZE, SEG_SIZE);
            }
            failed = false;
        } catch (RecordStoreException e) {
            throw new StorageException("Storage read failed: " + e.toString());
        } finally {
            close(store, failed);
        }
        // Publish only after all reads and close succeeded.
        cache = loaded;
    }

    private static void requireRecordCount(RecordStore store) throws RecordStoreException {
        if (store.getNumRecords() != SEGMENTS) {
            throw new StorageException("Invalid storage record count");
        }
    }

    private static void requireRange(int offset, int length) {
        if (offset < 0 || length < 0 || length > SIZE || offset > SIZE - length) {
            throw new IllegalArgumentException("Storage range: " + offset + "/" + length);
        }
    }

    private static void close(RecordStore store, boolean failed) {
        if (store == null) {
            return;
        }
        try {
            store.closeRecordStore();
        } catch (RecordStoreException e) {
            if (!failed) {
                throw new StorageException("Storage close failed: " + e.toString());
            }
            // Preserve the original failure rather than replace it with a cleanup failure.
        }
    }
}
