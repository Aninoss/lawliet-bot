package commands.runningchecker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import core.DiscordApiManager;
import core.cache.PatreonCache;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class RunningCheckerManager {

    private static final RunningCheckerManager ourInstance = new RunningCheckerManager();

    public static RunningCheckerManager getInstance() {
        return ourInstance;
    }

    private RunningCheckerManager() {
    }

    private final Cache<Long, ArrayList<RunningCheckerSlot>> runningCommandsCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();

    public synchronized boolean canUserRunCommand(Command command, long userId, int shardId, int maxCalculationTimeSec) {
        ArrayList<RunningCheckerSlot> runningCommandsList = runningCommandsCache.asMap().computeIfAbsent(userId, k -> new ArrayList<>());
        stopAndRemoveOutdatedRunningCommands(runningCommandsList);

        if (runningCommandsList.isEmpty() || runningCommandsList.size() < getMaxAmount(userId)) {
            final RunningCheckerSlot runningCheckerSlot = new RunningCheckerSlot(userId, shardId, maxCalculationTimeSec, command.hasTimeOut());
            runningCommandsList.add(runningCheckerSlot);
            removeOnThreadEnd(command, runningCommandsList, runningCheckerSlot, userId);

            return true;
        }

        return false;
    }

    private void removeOnThreadEnd(Command command, ArrayList<RunningCheckerSlot> runningCommandsList, RunningCheckerSlot runningCheckerSlot, long userId) {
        command.addCompletedListener(() -> {
            synchronized (this) {
                runningCommandsList.remove(runningCheckerSlot);
                if (runningCommandsList.isEmpty())
                    runningCommandsCache.invalidate(userId);
            }
        });
    }

    private int getMaxAmount(long userId) {
        return PatreonCache.getInstance().getUserTier(userId) >= 3 ? 2 : 1;
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
        return new HashMap<>(runningCommandsCache.asMap());
    }

}
