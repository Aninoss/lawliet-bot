package commands.runningchecker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import core.cache.PatreonCache;

public class RunningCheckerManager {

    private static final Cache<Long, ArrayList<RunningCheckerSlot>> runningCommandsCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();

    public static synchronized boolean canUserRunCommand(Command command, long guildId, long userId, int shardId, int maxCalculationTimeSec) {
        ArrayList<RunningCheckerSlot> runningCommandsList = runningCommandsCache.asMap().computeIfAbsent(userId, k -> new ArrayList<>());
        stopAndRemoveOutdatedRunningCommands(runningCommandsList);

        if (runningCommandsList.isEmpty() || runningCommandsList.size() < getMaxAmount(guildId, userId)) {
            final RunningCheckerSlot runningCheckerSlot = new RunningCheckerSlot(userId, shardId, maxCalculationTimeSec, !command.getCommandProperties().turnOffTimeout());
            runningCommandsList.add(runningCheckerSlot);
            removeOnThreadEnd(command, runningCommandsList, runningCheckerSlot, userId);

            return true;
        }

        return false;
    }

    private static void removeOnThreadEnd(Command command, ArrayList<RunningCheckerSlot> runningCommandsList, RunningCheckerSlot runningCheckerSlot, long userId) {
        command.addCompletedListener(() -> {
            remove(runningCommandsList, runningCheckerSlot, userId);
        });
    }

    private static synchronized void remove(ArrayList<RunningCheckerSlot> runningCommandsList, RunningCheckerSlot runningCheckerSlot, long userId) {
        runningCommandsList.remove(runningCheckerSlot);
        if (runningCommandsList.isEmpty()) {
            runningCommandsCache.invalidate(userId);
        }
    }

    private static int getMaxAmount(long guildId, long userId) {
        return PatreonCache.getInstance().hasPremium(userId, true) || PatreonCache.getInstance().isUnlocked(guildId) ? 2 : 1;
    }

    private static void stopAndRemoveOutdatedRunningCommands(ArrayList<RunningCheckerSlot> runningCommandsList) {
        new ArrayList<>(runningCommandsList).stream()
                .filter(runningCheckerSlot -> Instant.now().isAfter(runningCheckerSlot.getInstant().plusSeconds(runningCheckerSlot.getMaxCalculationTimeSec())))
                .forEach(runningCheckerSlot -> {
                    runningCheckerSlot.stop();
                    runningCommandsList.remove(runningCheckerSlot);
                });
    }

    public static synchronized HashMap<Long, ArrayList<RunningCheckerSlot>> getRunningCommandsMap() {
        return new HashMap<>(runningCommandsCache.asMap());
    }

}
