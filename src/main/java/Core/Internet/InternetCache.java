package Core.Internet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InternetCache {

    private static final HashMap<String, Instant> expirationDates = new HashMap<>();
    private static LoadingCache<String, CompletableFuture<InternetResponse>> cache = CacheBuilder.newBuilder()
            .maximumSize(300)
            .removalListener((removalNotification) -> expirationDates.remove((String)removalNotification.getKey()))
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
            cache.invalidate(url);
            expirationDates.put(url, Instant.now().plusSeconds(expirationTimeSeconds));
        }

        CompletableFuture<InternetResponse> future = cache.get(url);
        if (!future.isDone()) {
            future.thenAccept(internetResponse -> {
                if (internetResponse.getCode() / 100 != 2 && internetResponse.getCode() != 429) {
                    expirationDates.put(url, Instant.now().plus(5, ChronoUnit.MINUTES));
                }
            });
        }

        return future;
    }

    public static void setExpirationDate(Instant instant, String... urls) {
        for(String url: urls) expirationDates.put(url, instant);;
    }

}