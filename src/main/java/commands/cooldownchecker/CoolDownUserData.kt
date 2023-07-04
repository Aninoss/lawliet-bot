package commands.cooldownchecker

import constants.Settings
import core.schedule.MainScheduler
import java.time.Duration
import java.time.Instant
import java.util.*

class CoolDownUserData {

    private val commandInstants = ArrayList<Instant>()
    private var canPostCoolDownMessage = true

    fun getWaitingSec(coolDown: Int): Optional<Int> {
        clean()
        if (commandInstants.size >= Settings.COOLDOWN_MAX_ALLOWED) {
            val duration = Duration.between(Instant.now(), commandInstants[0])
            MainScheduler.schedule(commandInstants[0]) { canPostCoolDownMessage = true }
            return Optional.of((duration.seconds + 1).toInt())
        }
        commandInstants.add(Instant.now().plusSeconds(coolDown.toLong()))
        return Optional.empty()
    }

    @Synchronized
    fun canPostCoolDownMessage(): Boolean {
        if (canPostCoolDownMessage) {
            canPostCoolDownMessage = false
            return true
        }
        return false
    }

    private fun clean() {
        while (commandInstants.size > 0 && commandInstants[0].isBefore(Instant.now())) {
            commandInstants.removeAt(0)
        }
    }

    val isEmpty: Boolean
        get() {
            clean()
            return commandInstants.isEmpty()
        }
}