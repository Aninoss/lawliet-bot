package commands.runningchecker;

import java.time.Instant;

public class RunningCheckerSlot {

    private final long userId;
    private final Thread thread;
    private final int shardId;
    private final Instant instant;
    private final int maxCalculationTimeSec;
    private final boolean hasTimeOut;

    public RunningCheckerSlot(long userId, int shardId, int maxCalculationTimeSec, boolean hasTimeOut) {
        this.userId = userId;
        this.thread = Thread.currentThread();
        this.shardId = shardId;
        this.instant = Instant.now();
        this.maxCalculationTimeSec = maxCalculationTimeSec;
        this.hasTimeOut = hasTimeOut;
    }

    public long getUserId() {
        return userId;
    }

    public int getShardId() {
        return shardId;
    }

    public void stop() {
        if (hasTimeOut)
            thread.interrupt();
    }

    public Instant getInstant() {
        return instant;
    }

    public int getMaxCalculationTimeSec() { return maxCalculationTimeSec; }

}
