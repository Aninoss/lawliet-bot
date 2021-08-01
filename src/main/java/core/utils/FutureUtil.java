package core.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import core.GlobalThreadPool;

public class FutureUtil {

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        GlobalThreadPool.getExecutorService().submit(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

}
