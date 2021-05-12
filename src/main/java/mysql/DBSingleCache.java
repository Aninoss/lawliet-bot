package mysql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public abstract class DBSingleCache<T> extends DBCache {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(new CountingThreadFactory(() -> "Main", "SingleCache", false));
    private T o = null;
    private Instant nextUpdate = null;

    protected abstract T loadBean() throws Exception;

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

    public Integer getExpirationTimeMinutes() {
        return null;
    }

    @Override
    public void clear() {
        o = null;
    }

    @Override
    public void invalidateGuildId(long guildId) {
    }

}