package commands.runningchecker;

import core.CustomThread;
import core.PatreonCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCheckerManager {

    private static final RunningCheckerManager ourInstance = new RunningCheckerManager();
    public static RunningCheckerManager getInstance() { return ourInstance; }
    private RunningCheckerManager() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(RunningCheckerManager.class);

    private final HashMap<Long, ArrayList<RunningCheckerSlot>> runningCommandsMap = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId, int maxCalculationTimeSec) {
        ArrayList<RunningCheckerSlot> runningCommandsList = runningCommandsMap.computeIfAbsent(userId, k -> new ArrayList<>());
        stopAndRemoveOutdatedRunningCommands(runningCommandsList);

        if (runningCommandsList.isEmpty() || runningCommandsList.size() < getMaxAmount(userId)) {
            final RunningCheckerSlot runningCheckerSlot = new RunningCheckerSlot(userId, shardId, maxCalculationTimeSec);
            runningCommandsList.add(runningCheckerSlot);

            final Thread currentThread = Thread.currentThread();
            new CustomThread(() -> {
                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
                runningCommandsList.remove(runningCheckerSlot);
                if (runningCommandsList.size() == 0)
                    runningCommandsMap.remove(userId);
            }, "command_state_observer_thread", 1).start();

            return true;
        }

        return false;
    }

    private int getMaxAmount(long userId) {
        return PatreonCache.getInstance().getPatreonLevel(userId) >= 3 ? 2 : 1;
    }

    private void stopAndRemoveOutdatedRunningCommands(ArrayList<RunningCheckerSlot> runningCommandsList) {
        new ArrayList<>(runningCommandsList).stream()
                .filter(runningCheckerSlot -> Instant.now().isAfter(runningCheckerSlot.getInstant().plusSeconds(runningCheckerSlot.getMaxCalculationTimeSec())))
                .forEach(runningCheckerSlot -> {
                    runningCheckerSlot.stop();
                    runningCommandsList.remove(runningCheckerSlot);
                });
    }

    public synchronized HashMap<Long, ArrayList<RunningCheckerSlot>> getRunningCommandsMap() {
        return new HashMap<>(runningCommandsMap);
    }

    public synchronized void clear() {
        runningCommandsMap.clear();
    }

    public synchronized void clearShard(int shardId) {
        runningCommandsMap.values().forEach(runningCommandsList -> runningCommandsList.removeIf(rc -> rc.getShardId() == shardId));
    }

}
