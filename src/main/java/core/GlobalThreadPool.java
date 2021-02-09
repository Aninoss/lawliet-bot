package core;

import java.util.concurrent.*;

public class GlobalThreadPool {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
