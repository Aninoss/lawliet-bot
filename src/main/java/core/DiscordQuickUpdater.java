package core;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DiscordQuickUpdater {

    private static final DiscordQuickUpdater ourInstance = new DiscordQuickUpdater();
    public static DiscordQuickUpdater getInstance() { return ourInstance; }
    private DiscordQuickUpdater() { }

    private final HashMap<String, HashMap<Object, CompletableFuture<?>>> typeMap = new HashMap<>();

    public void update(String type, Object key, Supplier<CompletableFuture<?>> supplier) {
        HashMap<Object, CompletableFuture<?>> futureMap = typeMap.computeIfAbsent(type, k -> new HashMap<>());

        futureMap.computeIfPresent(key, (k, future) -> {
            future.cancel(true);
            return future;
        });

        CompletableFuture<?> future = supplier.get();

        if (future != null) {
            futureMap.put(key, future);
            future.thenRun(() -> futureMap.remove(key));
        }
    }

}
