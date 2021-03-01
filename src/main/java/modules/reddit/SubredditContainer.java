package modules.reddit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class SubredditContainer {

    private static final SubredditContainer ourInstance = new SubredditContainer();

    public static SubredditContainer getInstance() {
        return ourInstance;
    }

    private SubredditContainer() {
    }

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