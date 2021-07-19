package mysql;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class DBMapCache<T, U> extends DBCache {

    private final LoadingCache<T, U> cache;

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(12));
    }

    protected DBMapCache() {
        cache = getCacheBuilder().build(
                new CacheLoader<>() {
                    @Override
                    public U load(@NonNull T t) throws Exception {
                        return process(t);
                    }
                }
        );
    }

    protected abstract U load(T t) throws Exception;

    protected U process(T t) throws Exception {
        return DBMapCache.this.load(t);
    }

    public U retrieve(T t) {
        try {
            return cache.get(t);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected LoadingCache<T, U> getCache() {
        return cache;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void invalidateGuildId(long guildId) {
        if (cache.asMap().keySet().stream().findFirst().map(key -> key instanceof Long).orElse(false)) {
            cache.invalidate(guildId);
        }
    }

}
