package core;

import org.javacord.api.util.ratelimit.Ratelimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SendEvent;

import java.util.concurrent.ExecutionException;

public class CustomLocalRatelimiter implements Ratelimiter {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomLocalRatelimiter.class);

    private volatile long nextRequest = 0;

    private final long nanos;

    public CustomLocalRatelimiter(long nanos) {
        this.nanos = nanos;
    }

    @Override
    public synchronized void requestQuota() throws InterruptedException {
        if (System.nanoTime() < nextRequest) {
            long sleepTime;
            while ((sleepTime = calculateLocalSleepTime()) > 0) { // Sleep is unreliable, so we have to loop
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            }
        }

        if (Bot.isProductionMode()) {
            while (true) {
                try {
                    long syncedSleepTime = SendEvent.sendRequestSyncedRatelimit().get();
                    Thread.sleep(syncedSleepTime / 1_000_000, (int) (syncedSleepTime % 1_000_000));
                    break;
                } catch (ExecutionException e) {
                    LOGGER.error("Error when requesting synced waiting time", e);
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
