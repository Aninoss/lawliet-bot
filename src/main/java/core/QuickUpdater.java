package core;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.requests.RestAction;

public class QuickUpdater {

    private static final QuickUpdater ourInstance = new QuickUpdater();

    public static QuickUpdater getInstance() {
        return ourInstance;
    }

    private QuickUpdater() {
    }

    private final Cache<String, CompletableFuture<?>> futureCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(20))
            .build();

    public synchronized void update(String type, Object key, RestAction<?> restAction) {
        String stringKey = type + ":" + key;

        CompletableFuture<?> oldFuture = futureCache.getIfPresent(stringKey);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }

        CompletableFuture<?> future = restAction.submit();
        futureCache.put(stringKey, future);

        future.exceptionally(e -> {
            if (!(e instanceof CancellationException)) {
                MainLogger.get().error("Exception in quick updater", e);
            }
            return null;
        }).thenAccept(result -> futureCache.asMap().remove(stringKey, future));
    }

}
