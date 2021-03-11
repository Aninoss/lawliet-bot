package core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalThreadPool {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
