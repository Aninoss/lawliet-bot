package CommandSupporters.RunningCommands;

import Constants.Settings;
import Core.CustomThread;
import javafx.util.Pair;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCommandManager {

    private static final RunningCommandManager ourInstance = new RunningCommandManager();
    public static RunningCommandManager getInstance() { return ourInstance; }
    private RunningCommandManager() {}

    final static Logger LOGGER = LoggerFactory.getLogger(RunningCommandManager.class);

    private HashMap<Long, RunningCommand> runningCommands = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId, int maxCalculationTimeSec) {
        RunningCommand runningCommand = runningCommands.get(userId);

        if (runningCommand == null || Instant.now().isAfter(runningCommand.getInstant().plusSeconds(runningCommand.getMaxCalculationTimeSec()))) {
            if (runningCommand != null) runningCommand.stop();
            runningCommands.put(userId, new RunningCommand(userId, shardId, maxCalculationTimeSec));

            final Thread currentThread = Thread.currentThread();
            Thread t = new CustomThread(() -> {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
                runningCommands.remove(userId);
            }, "command_state_observer_thread", 1);
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
