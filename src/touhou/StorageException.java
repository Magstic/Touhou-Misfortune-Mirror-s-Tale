package touhou;

/** An RMS operation or stored data failed; callers must not substitute defaults. */
public final class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
}
