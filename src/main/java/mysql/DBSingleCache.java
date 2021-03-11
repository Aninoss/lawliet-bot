package mysql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class DBSingleCache<T> extends DBCache {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private T o = null;
    private Instant nextUpdate = null;

    public synchronized T retrieve() {
        if (o == null) {
            try {
                o = loadBean();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            setExpirationTimer();
        } else if (nextUpdate != null && Instant.now().isAfter(nextUpdate)) {
            setExpirationTimer();
            executorService.submit(() -> {
                try {
                    o = loadBean();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return o;
    }

    private void setExpirationTimer() {
        Integer expirationTimeMinutes = getExpirationTimeMinutes();
        if (expirationTimeMinutes != null) {
            nextUpdate = Instant.now().plus(expirationTimeMinutes, ChronoUnit.MINUTES);
        }
    }

    public boolean isCached() {
        return o != null;
    }

    @Override
    public void clear() {
        o = null;
    }

    public Integer getExpirationTimeMinutes() {
        return null;
    }

    protected abstract T loadBean() throws Exception;

}