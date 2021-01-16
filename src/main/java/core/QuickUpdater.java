package core;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class QuickUpdater {

    private static final QuickUpdater ourInstance = new QuickUpdater();
    public static QuickUpdater getInstance() { return ourInstance; }
    private QuickUpdater() { }

    private final HashMap<String, Supplier<CompletableFuture<?>>> supplierMap = new HashMap<>();

    public synchronized void update(String type, Object key, Supplier<CompletableFuture<?>> supplier) {
        if (supplier != null) {
            String stringKey = type + ":" + key;

            Supplier<CompletableFuture<?>> oldSupplier = supplierMap.get(stringKey);
            if (oldSupplier == null) {
                executeSupplier(stringKey, supplier);
            } else {
                supplierMap.put(stringKey, supplier);
            }
        }
    }

    private void executeSupplier(String key, Supplier<CompletableFuture<?>> currentSupplier) {
        supplierMap.put(key, currentSupplier);
        CompletableFuture<?> future = currentSupplier.get();
        if (future != null) {
            future.thenRun(() -> {
                Supplier<CompletableFuture<?>> newSupplier = supplierMap.get(key);
                if (!newSupplier.equals(currentSupplier)) {
                    executeSupplier(key, newSupplier);
                } else {
                    supplierMap.remove(key);
                }
            });
        }
    }

}
