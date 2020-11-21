package modules.osu;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OsuAccountSync {

    private static final OsuAccountSync ourInstance = new OsuAccountSync();
    public static OsuAccountSync getInstance() { return ourInstance; }
    private OsuAccountSync() { }

    private final Cache<Long, Consumer<String>> osuSyncCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public void add(long userId, Consumer<String> action) {
        osuSyncCache.put(userId, action);
    }

    public Optional<Consumer<String>> getUserInCache(long userId) {
        return Optional.ofNullable(osuSyncCache.getIfPresent(userId));
    }

    public void remove(long userId) {
        osuSyncCache.invalidate(userId);
    }

}
