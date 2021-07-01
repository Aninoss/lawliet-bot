package modules.porn;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PornImageCache {

    private static final PornImageCache ourInstance = new PornImageCache();

    public static PornImageCache getInstance() {
        return ourInstance;
    }

    private PornImageCache() {
    }

    private final LoadingCache<Integer, PornImageCacheSearchKey> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public PornImageCacheSearchKey load(@NonNull Integer hash) {
                            return new PornImageCacheSearchKey();
                        }
                    }
            );

    public PornImageCacheSearchKey get(long guildId, @NonNull String domain, @NonNull String searchKey) {
        try {
            int hash = Objects.hash(guildId, domain, searchKey.toLowerCase());
            return cache.get(hash);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}