package core;

import org.javacord.api.util.ratelimit.Ratelimiter;

public class CustomLocalRatelimiter implements Ratelimiter {

    private volatile long nextResetNanos;
    private volatile int remainingQuota;

    private final int amount;
    private final int nanos;

    public CustomLocalRatelimiter(int amount, int nanos) {
        this.amount = amount;
        this.nanos = nanos;
    }

    @Override
    public synchronized void requestQuota() throws InterruptedException {
        if (remainingQuota <= 0) {
            // Wait until a new quota becomes available
            long sleepTime;
            while ((sleepTime = calculateSleepTime()) > 0) { // Sleep is unreliable, so we have to loop
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            }
        }

        // Reset the limit when the last reset timestamp is past
        if (System.nanoTime() > nextResetNanos) {
            remainingQuota = amount;
            nextResetNanos = System.nanoTime() + nanos;
        }

        remainingQuota--;
    }

    private long calculateSleepTime() {
        return (nextResetNanos - System.nanoTime());
    }
}
