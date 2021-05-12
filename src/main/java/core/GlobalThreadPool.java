package core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class GlobalThreadPool {

    private static final ExecutorService executorService = Executors.newCachedThreadPool(new CountingThreadFactory(() -> "Main", "ThreadPool", false));

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
