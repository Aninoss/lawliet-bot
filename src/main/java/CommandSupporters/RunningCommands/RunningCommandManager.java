package CommandSupporters.RunningCommands;

import Core.CustomThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;

public class RunningCommandManager {

    private static final RunningCommandManager ourInstance = new RunningCommandManager();
    public static RunningCommandManager getInstance() { return ourInstance; }
    private RunningCommandManager() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(RunningCommandManager.class);

    private HashMap<Long, RunningCommand> runningCommands = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId, int maxCalculationTimeSec) {
        RunningCommand runningCommand = runningCommands.get(userId);

        if (runningCommand == null || Instant.now().isAfter(runningCommand.getInstant().plusSeconds(runningCommand.getMaxCalculationTimeSec()))) {
            if (runningCommand != null) runningCommand.stop();
            runningCommands.put(userId, new RunningCommand(userId, shardId, maxCalculationTimeSec));

            final Thread currentThread = Thread.currentThread();
            new CustomThread(() -> {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
                runningCommands.remove(userId);
            }, "command_state_observer_thread", 1).start();

            return true;
        }

        return false;
    }

    public HashMap<Long, RunningCommand> getRunningCommands() {
        return new HashMap<>(runningCommands);
    }

    public void clear() {
        runningCommands = new HashMap<>();
    }

    public synchronized void clearShard(int shardId) {
        runningCommands.entrySet().removeIf(set -> set.getValue().getShardId() == shardId);
    }

}
