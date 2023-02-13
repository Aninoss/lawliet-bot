package commands.runningchecker

import com.google.common.cache.CacheBuilder
import commands.Command
import core.cache.PatreonCache
import java.time.Duration
import java.time.Instant

object RunningCheckerManager {

    private val runningCommandsCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build<Long, ArrayList<RunningCheckerSlot>>()

    @JvmStatic
    @get:Synchronized
    val runningCommandsMap: HashMap<Long, ArrayList<RunningCheckerSlot>>
        get() = HashMap(runningCommandsCache.asMap())

    @JvmStatic
    @Synchronized
    fun canUserRunCommand(command: Command, guildId: Long, userId: Long, shardId: Int, maxCalculationTimeSec: Int): Boolean {
        val runningCommandsList = runningCommandsCache.asMap().computeIfAbsent(userId) { ArrayList() }
        stopAndRemoveOutdatedRunningCommands(runningCommandsList)
        if (runningCommandsList.isEmpty() || runningCommandsList.size < getMaxAmount(guildId, userId)) {
            val runningCheckerSlot = RunningCheckerSlot(userId, shardId, maxCalculationTimeSec, command.commandProperties.enableCacheWipe)
            runningCommandsList.add(runningCheckerSlot)
            removeOnThreadEnd(command, runningCommandsList, runningCheckerSlot, userId)
            return true
        }
        return false
    }

    private fun removeOnThreadEnd(command: Command, runningCommandsList: ArrayList<RunningCheckerSlot>, runningCheckerSlot: RunningCheckerSlot,
                                  userId: Long
    ) {
        command.addCompletedListener { remove(runningCommandsList, runningCheckerSlot, userId) }
    }

    @Synchronized
    private fun remove(runningCommandsList: ArrayList<RunningCheckerSlot>, runningCheckerSlot: RunningCheckerSlot, userId: Long) {
        runningCommandsList.remove(runningCheckerSlot)
        if (runningCommandsList.isEmpty()) {
            runningCommandsCache.invalidate(userId)
        }
    }

    private fun getMaxAmount(guildId: Long, userId: Long): Int {
        return if (PatreonCache.getInstance().hasPremium(userId, true) || PatreonCache.getInstance().isUnlocked(guildId)) 2 else 1
    }

    private fun stopAndRemoveOutdatedRunningCommands(runningCommandsList: ArrayList<RunningCheckerSlot>) {
        ArrayList(runningCommandsList).stream()
            .filter { runningCheckerSlot: RunningCheckerSlot -> Instant.now().isAfter(runningCheckerSlot.instant.plusSeconds(runningCheckerSlot.maxCalculationTimeSec.toLong())) }
            .forEach { runningCheckerSlot: RunningCheckerSlot ->
                runningCheckerSlot.stop()
                runningCommandsList.remove(runningCheckerSlot)
            }
    }

}