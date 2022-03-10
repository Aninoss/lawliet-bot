package core;

import java.util.concurrent.*;
import websockets.syncserver.SendEvent;
import websockets.syncserver.SyncManager;

public class SyncConnectionChecker {

    public static void start() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                if (Program.productionMode() && !isConnected()) {
                    MainLogger.get().error("Sync websocket disconnected, attempting reconnect");
                    SyncManager.reconnect();
                }
            } catch (Throwable e) {
                //ignore
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static boolean isConnected() {
        if (!SyncManager.getClient().isConnected()) {
            return false;
        }

        try {
            return SendEvent.sendPing().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            //ignore
            return false;
        }
    }

}
