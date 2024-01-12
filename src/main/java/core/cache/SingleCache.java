package core.cache;

import core.GlobalThreadPool;
import core.MainLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class SingleCache<T> {

    private Instant nextReset = null;
    private T value = null;
    private boolean fetchAccess = true;

    public synchronized T get() {
        if (value == null || nextReset == null || Instant.now().isAfter(nextReset)) {
            return fetch();
        }

        return value;
    }

    public synchronized T getAsync() {
        if (value == null) {
            return get();
        }

        if (nextReset == null || Instant.now().isAfter(nextReset)) {
            resetUpdateTimer();
            GlobalThreadPool.submit(this::fetch);
        }

        return value;
    }

    public void resetUpdateTimer() {
        nextReset = Instant.now().plus(getRefreshRateMinutes(), ChronoUnit.MINUTES);
    }

    public T fetch() {
        boolean fetch = fetchAccess;
        synchronized (this) {
            if (fetch) {
                fetchAccess = false;
                resetUpdateTimer();
                try {
                    T newValue = fetchValue();
                    if (newValue != null) {
                        this.value = newValue;
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Uncaught exception", e);
                } finally {
                    fetchAccess = true;
                }
            }
        }
        return this.value;
    }

    public void setValue(T value) {
        resetUpdateTimer();
        this.value = value;
    }

    protected int getRefreshRateMinutes() {
        return 5;
    }

    protected abstract T fetchValue();

}
