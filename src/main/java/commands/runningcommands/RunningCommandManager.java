package commands.runningcommands;

import core.CustomThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCommandManager {

    private static final RunningCommandManager ourInstance = new RunningCommandManager();
    public static RunningCommandManager getInstance() { return ourInstance; }
    private RunningCommandManager() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(RunningCommandManager.class);

    private final HashMap<Long, ArrayList<RunningCommand>> runningCommandsMap = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId, int maxCalculationTimeSec, int maxAmount) {
        ArrayList<RunningCommand> runningCommandsList = runningCommandsMap.computeIfAbsent(userId, k -> new ArrayList<>());
        stopAndRemoveOutdatedRunningCommands(runningCommandsList);

        if (runningCommandsList.size() < maxAmount) {
            final RunningCommand runningCommand = new RunningCommand(userId, shardId, maxCalculationTimeSec);
            runningCommandsList.add(runningCommand);

            final Thread currentThread = Thread.currentThread();
            new CustomThread(() -> {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
                runningCommandsList.remove(runningCommand);
                if (runningCommandsList.size() == 0)
                    runningCommandsMap.remove(userId);
            }, "command_state_observer_thread", 1).start();

            return true;
        }

        return false;
    }

    private void stopAndRemoveOutdatedRunningCommands(ArrayList<RunningCommand> runningCommandsList) {
        new ArrayList<>(runningCommandsList).stream()
                .filter(runningCommand -> Instant.now().isAfter(runningCommand.getInstant().plusSeconds(runningCommand.getMaxCalculationTimeSec())))
                .forEach(runningCommand -> {
                    runningCommand.stop();
                    runningCommandsList.remove(runningCommand);
                });
    }

    public HashMap<Long, ArrayList<RunningCommand>> getRunningCommandsMap() {
        return new HashMap<>(runningCommandsMap);
    }

    public synchronized void clear() {
        runningCommandsMap.clear();
    }

    public synchronized void clearShard(int shardId) {
        runningCommandsMap.values().forEach(runningCommandsList -> runningCommandsList.removeIf(rc -> rc.getShardId() == shardId));
    }

}
