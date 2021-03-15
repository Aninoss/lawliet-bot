package core;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.requests.RestAction;

public class QuickUpdater {

    private final Cache<Long, CompletableFuture<?>> futureCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(20))
            .build();

    public synchronized void update(long key, RestAction<?> restAction) {
        CompletableFuture<?> oldFuture = futureCache.getIfPresent(key);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }

        CompletableFuture<?> future = restAction.submitAfter(500, TimeUnit.MILLISECONDS);
        futureCache.put(key, future);

        future.exceptionally(e -> {
            if (!(e instanceof CancellationException)) {
                MainLogger.get().error("Exception in quick updater", e);
            }
            return null;
        }).thenAccept(result -> futureCache.asMap().remove(key, future));
    }

}
