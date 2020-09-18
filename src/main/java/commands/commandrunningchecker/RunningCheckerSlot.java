package commands.commandrunningchecker;

import java.time.Instant;

public class RunningCheckerSlot {

    private final long userId;
    private final Thread thread;
    private final int shardId;
    private final Instant instant;
    private final int maxCalculationTimeSec;

    public RunningCheckerSlot(long userId, int shardId, int maxCalculationTimeSec) {
        this.userId = userId;
        this.thread = Thread.currentThread();
        this.shardId = shardId;
        this.instant = Instant.now();
        this.maxCalculationTimeSec = maxCalculationTimeSec;
    }

    public long getUserId() {
        return userId;
    }

    public int getShardId() {
        return shardId;
    }

    public void stop() {
        thread.interrupt();
    }

    public Instant getInstant() {
        return instant;
    }

    public int getMaxCalculationTimeSec() { return maxCalculationTimeSec; }

}
