package core;

import java.util.concurrent.*;

public class GlobalThreadPool {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(50);

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
