package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class QuickUpdater {

    private static final QuickUpdater ourInstance = new QuickUpdater();

    public static QuickUpdater getInstance() {
        return ourInstance;
    }

    private QuickUpdater() {
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(QuickUpdater.class);

    private final Cache<String, Supplier<CompletableFuture<?>>> supplierMap = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(20))
            .build();

    public synchronized void update(String type, Object key, Supplier<CompletableFuture<?>> supplier) {
        if (supplier != null) {
            String stringKey = type + ":" + key;

            Supplier<CompletableFuture<?>> oldSupplier = supplierMap.getIfPresent(stringKey);
            if (oldSupplier == null) {
                executeSupplier(stringKey, supplier);
            } else {
                supplierMap.put(stringKey, supplier);
            }
        }
    }

    private void executeSupplier(String key, Supplier<CompletableFuture<?>> currentSupplier) {
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
        Supplier<CompletableFuture<?>> newSupplier = supplierMap.getIfPresent(key);
        if (!currentSupplier.equals(newSupplier)) {
            executeSupplier(key, newSupplier);
        } else {
            supplierMap.invalidate(key);
        }
    }

}
