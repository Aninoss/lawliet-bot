package core.internet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class InternetCache {

    private static final LoadingCache<String, CompletableFuture<HttpResponse>> shortLivedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .softValues()
            .maximumSize(300)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public CompletableFuture<HttpResponse> load(@NonNull String url) {
                            System.out.println("Cache");//TODO
                            return HttpRequest.getData(url);
                        }
                    });

    private static final HashMap<String, Instant> expirationDates = new HashMap<>();
    private static final LoadingCache<String, CompletableFuture<HttpResponse>> cache = CacheBuilder.newBuilder()
            .removalListener((removalNotification) -> expirationDates.remove((String)removalNotification.getKey()))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public CompletableFuture<HttpResponse> load(@NonNull String url) {
                            return HttpRequest.getData(url);
                        }
                    });


    public static CompletableFuture<HttpResponse> getData(String url) {
        return getData(url, 60 * 5);
    }

    public static CompletableFuture<HttpResponse> getDataShortLived(String url) {
        try {
            return shortLivedCache.get(url);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<HttpResponse> getData(String url, int expirationTimeSeconds) {
        if (!expirationDates.containsKey(url) || expirationDates.get(url).isBefore(Instant.now())) {
            cache.invalidate(url);
            expirationDates.put(url, Instant.now().plusSeconds(expirationTimeSeconds));
        }

        try {
            return cache.get(url);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setExpirationDate(Instant instant, String... urls) {
        for(String url: urls)
            expirationDates.put(url, instant);
    }

}