package mysql;

import java.time.Duration;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class DBMapCache<T, U extends Observable> extends DBCache implements Observer {

    private final DBMapCache<T, U> instance = this;
    private final LoadingCache<T, U> cache;

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10));
    }

    protected DBMapCache() {
        cache = getCacheBuilder().build(
                new CacheLoader<>() {
                    @Override
                    public U load(@NonNull T t) throws Exception {
                        U u = DBMapCache.this.load(t);
                        u.addObserver(instance);
                        return u;
                    }
                }
        );
    }

    protected abstract U load(T t) throws Exception;

    protected abstract void save(U u);

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
    public void update(Observable o, Object arg) {
        save((U) o);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

}
