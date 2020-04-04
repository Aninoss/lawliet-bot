package CommandSupporters.RunningCommands;

import javafx.util.Pair;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCommandManager {

    private static RunningCommandManager ourInstance = new RunningCommandManager();
    public static RunningCommandManager getInstance() {
        return ourInstance;
    }
    private RunningCommandManager() {}

    private HashMap<Long, RunningCommand> runningCommands = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId) {
        RunningCommand runningCommand = runningCommands.get(userId);

        if (runningCommand == null || runningCommand.getInstant().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            if (runningCommand != null) runningCommand.stop();
            runningCommands.put(userId, new RunningCommand(userId, shardId));

            final Thread currentThread = Thread.currentThread();
            Thread t = new Thread(() -> {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runningCommands.remove(userId);
            });
            t.setName("command_state_observer_thread");
            t.setPriority(1);
            t.start();

            return true;
        }

        return false;
    }

    public HashMap<Long, RunningCommand> getRunningCommands() {
        return new HashMap<>(runningCommands);
    }

    public boolean isActive(long userId, Thread commandThread) {
        return runningCommands.containsKey(userId) && runningCommands.get(userId).getThread() == commandThread;
    }

    public void clear() {
        runningCommands = new HashMap<>();
    }

    public synchronized void clearShard(int shardId) {
        runningCommands.entrySet().removeIf(set -> set.getValue().getShardId() == shardId);
    }

}
