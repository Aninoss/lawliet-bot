package core.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import core.GlobalThreadPool;
import lombok.extern.log4j.Log4j2;

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
            GlobalThreadPool.getExecutorService().submit(this::fetch);
        }

        return value;
    }

    public void resetUpdateTimer() {
        nextReset = Instant.now().plus(getRefreshRateMinutes(), ChronoUnit.MINUTES);
    }

    public T fetch() {
        resetUpdateTimer();
        try {
            T newValue = fetchValue();
            if (newValue != null)
                this.value = newValue;
        } catch (Throwable e) {
            MainLogger.get().error("Uncaught exception", e);
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
