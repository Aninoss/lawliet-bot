package core;

import java.util.concurrent.ExecutionException;
import websockets.syncserver.SendEvent;

public class CustomLocalRatelimiter {

    private volatile long nextRequest = 0;

    private final long nanos;

    public CustomLocalRatelimiter(double millis) {
        this.nanos = Math.round(millis * 1_000_000);
    }

    public synchronized void requestQuota() throws InterruptedException {
        if (System.nanoTime() < nextRequest) {
            long sleepTime;
            while ((sleepTime = calculateLocalSleepTime()) > 0) { // Sleep is unreliable, so we have to loop
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            }
        }

        if (Program.isProductionMode()) {
            while (true) {
                try {
                    long syncedSleepTime = SendEvent.sendRequestSyncedRatelimit().get();
                    Thread.sleep(syncedSleepTime / 1_000_000, (int) (syncedSleepTime % 1_000_000));
                    break;
                } catch (ExecutionException e) {
                    MainLogger.get().error("Error when requesting synced waiting time", e);
                    Thread.sleep(5000);
                }
            }
        }

        nextRequest = System.nanoTime() + nanos;
    }

    private long calculateLocalSleepTime() {
        return (nextRequest - System.nanoTime());
    }

}
