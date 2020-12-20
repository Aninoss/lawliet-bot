package mysql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class DBSingleBeanGenerator<T> extends DBCached {

    private T o = null;
    private Instant nextUpdate = null;

    public T getBean() {
        if (o == null || (nextUpdate != null && Instant.now().isAfter(nextUpdate))) {
            try {
                o = loadBean();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            Integer expirationTimeMinutes = getExpirationTimeMinutes();
            if (expirationTimeMinutes != null)
                nextUpdate = Instant.now().plus(expirationTimeMinutes, ChronoUnit.MINUTES);
        }
        return o;
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