package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class QuickUpdater {

    private static final QuickUpdater ourInstance = new QuickUpdater();
    public static QuickUpdater getInstance() { return ourInstance; }
    private QuickUpdater() { }

    private final static Logger LOGGER = LoggerFactory.getLogger(QuickUpdater.class);

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
        if (key.endsWith("638158050972401664"))
            LOGGER.info("MCDisplay QU start"); //TODO

        supplierMap.put(key, currentSupplier);
        CompletableFuture<?> future = null;
        try {
            future = currentSupplier.get();
        } catch (Throwable e) {
            LOGGER.error("Exception", e);
        }

        if (future != null) {
            future.thenRun(() -> {
                executeNext(key, currentSupplier);
            });
        } else {
            executeNext(key, currentSupplier);
        }
    }

    private void executeNext(String key, Supplier<CompletableFuture<?>> currentSupplier) {
        if (key.endsWith("638158050972401664"))
            LOGGER.info("MCDisplay QU next"); //TODO

        Supplier<CompletableFuture<?>> newSupplier = supplierMap.get(key);
        if (!newSupplier.equals(currentSupplier)) {
            executeSupplier(key, newSupplier);
        } else {
            supplierMap.remove(key);
        }
    }

}
