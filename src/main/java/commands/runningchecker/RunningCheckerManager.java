package commands.runningchecker;

import core.NewApiCollection;
import core.cache.PatreonCache;
import core.schedule.MainScheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCheckerManager {

    private static final RunningCheckerManager ourInstance = new RunningCheckerManager();
    public static RunningCheckerManager getInstance() { return ourInstance; }

    private RunningCheckerManager() {
        NewApiCollection.getInstance().addShardDisconnectConsumer(this::clearShard);
    }

    private final HashMap<Long, ArrayList<RunningCheckerSlot>> runningCommandsMap = new HashMap<>();

    public synchronized boolean canUserRunCommand(long userId, int shardId, int maxCalculationTimeSec) {
        ArrayList<RunningCheckerSlot> runningCommandsList = runningCommandsMap.computeIfAbsent(userId, k -> new ArrayList<>());
        stopAndRemoveOutdatedRunningCommands(runningCommandsList);

        if (runningCommandsList.isEmpty() || runningCommandsList.size() < getMaxAmount(userId)) {
            final RunningCheckerSlot runningCheckerSlot = new RunningCheckerSlot(userId, shardId, maxCalculationTimeSec);
            runningCommandsList.add(runningCheckerSlot);
            removeOnThreadEnd(runningCommandsList, runningCheckerSlot, userId);

            return true;
        }

        return false;
    }

    private void removeOnThreadEnd(ArrayList<RunningCheckerSlot> runningCommandsList, RunningCheckerSlot runningCheckerSlot, long userId) {
        final Thread currentThread = Thread.currentThread();
        MainScheduler.getInstance().poll(100, "runningchecker", () -> {
            if (currentThread.isAlive())
                return true;

            synchronized (this) {
                runningCommandsList.remove(runningCheckerSlot);
                if (runningCommandsList.isEmpty())
                    runningCommandsMap.remove(userId);
                return false;
            }
        });
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
