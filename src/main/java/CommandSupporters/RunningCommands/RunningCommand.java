package CommandSupporters.RunningCommands;

import org.javacord.api.entity.user.User;

import java.time.Instant;

public class RunningCommand {

    private final long userId;
    private final Thread thread;
    private final int shardId;
    private final Instant instant;

    public RunningCommand(long userId, int shardId) {
        this.userId = userId;
        this.thread = Thread.currentThread();
        this.shardId = shardId;
        this.instant = Instant.now();
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

    public Thread getThread() {
        return thread;
    }
}
