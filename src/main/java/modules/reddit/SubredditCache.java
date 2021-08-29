package modules.reddit;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SubredditCache {

    private final LoadingCache<String, Subreddit> subredditCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(50)
            .build(new CacheLoader<>() {
                @Override
                public Subreddit load(@NonNull String subredditName) {
                    return new Subreddit(subredditName);
                }
            });

    public Subreddit get(String name) {
        try {
            return subredditCache.get(name.toLowerCase());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}