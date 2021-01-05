package core;

import java.util.concurrent.*;

public class GlobalThreadPool {

    private static final ExecutorService executorService = new ThreadPoolExecutor(0, 50,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
