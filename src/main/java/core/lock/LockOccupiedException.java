package core.lock;

public class LockOccupiedException extends Exception {

    public LockOccupiedException() {
    }

    public LockOccupiedException(String message) {
        super(message);
    }

    public LockOccupiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockOccupiedException(Throwable cause) {
        super(cause);
    }

    public LockOccupiedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
