package core.cache;

import core.CustomThread;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class SingleCache <T> {

    private Instant nextReset = null;
    private T value = null;

    public synchronized T get() {
        if (nextReset == null || Instant.now().isAfter(nextReset)) {
            return fetch();
        }

        return value;
    }

    public synchronized T getAsync() {
        if (value == null)
            return get();

        if (nextReset == null || Instant.now().isAfter(nextReset)) {
            resetUpdateTimer();
            new CustomThread(this::fetch, "singlecache_refresh", 1).start();
        }

        return value;
    }

    public void resetUpdateTimer() {
        nextReset = Instant.now().plus(getRefreshRateMinutes(), ChronoUnit.MINUTES);
    }

    public T fetch() {
        resetUpdateTimer();
        T newValue = fetchValue();
        if (newValue != null)
            this.value = newValue;
        return this.value;
    }

    protected int getRefreshRateMinutes() {
        return 5;
    }

    protected abstract T fetchValue();

}
