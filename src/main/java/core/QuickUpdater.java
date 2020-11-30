package core;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class QuickUpdater {

    private static final QuickUpdater ourInstance = new QuickUpdater();
    public static QuickUpdater getInstance() { return ourInstance; }
    private QuickUpdater() { }

    private final HashMap<String, CompletableFuture<?>> futureMap = new HashMap<>();

    public void update(String type, Object key, Supplier<CompletableFuture<?>> supplier) {
        String stringKey = type + ":" + key;

        futureMap.computeIfPresent(stringKey, (k, future) -> {
            future.cancel(true);
            return future;
        });

        CompletableFuture<?> future = supplier.get();

        if (future != null) {
            futureMap.put(stringKey, future);
            future.thenRun(() -> futureMap.remove(stringKey));
        }
    }

}
