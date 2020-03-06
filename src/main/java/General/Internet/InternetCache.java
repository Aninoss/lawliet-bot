package General.Internet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InternetCache {

    private static HashMap<String, Instant> expirationDates = new HashMap<>();
    private static LoadingCache<String, CompletableFuture<InternetResponse>> cache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .removalListener((removalNotification) -> removeExpirationDate((String) removalNotification.getKey()))
            .build(
                    new CacheLoader<String, CompletableFuture<InternetResponse>>() {
                        @Override
                        public CompletableFuture<InternetResponse> load(@NonNull String url) throws IOException {
                            return Internet.getData(url);
                        }
                    });

    public static CompletableFuture<InternetResponse> getData(String url) throws ExecutionException {
        return getData(url, 60 * 5);
    }

    public static CompletableFuture<InternetResponse> getData(String url, int expirationTimeSeconds) throws ExecutionException {
        if (!expirationDates.containsKey(url) || expirationDates.get(url).isBefore(Instant.now())) {
            expirationDates.put(url, Instant.now().plusSeconds(expirationTimeSeconds));
            cache.invalidate(url);
        }
        return cache.get(url);
    }

    public static void setExpirationDate(Instant instant, String... urls) {
        for(String url: urls) setExpirationDate(url, instant);
    }

    public static void setExpirationDate(String url, Instant instant) {
        expirationDates.put(url, instant);
    }

    private static void removeExpirationDate(String url) {
        expirationDates.remove(url);
    }

}