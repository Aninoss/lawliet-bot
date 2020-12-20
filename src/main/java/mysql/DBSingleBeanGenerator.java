package mysql;

import core.TaskQueue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class DBSingleBeanGenerator<T> extends DBCached {

    private static final TaskQueue refreshQueue = new TaskQueue("mysql_refresh");
    private T o = null;
    private Instant nextUpdate = null;

    public synchronized T getBean() {
        if (o == null) {
            try {
                o = loadBean();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            setExpirationTimer();
        } else if (nextUpdate != null && Instant.now().isAfter(nextUpdate)) {
            setExpirationTimer();
            refreshQueue.attach(() -> {
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
        if (expirationTimeMinutes != null)
            nextUpdate = Instant.now().plus(expirationTimeMinutes, ChronoUnit.MINUTES);
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