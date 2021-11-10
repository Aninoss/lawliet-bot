package commands.runningchecker

import java.time.Instant

class RunningCheckerSlot(val userId: Long, shardId: Int, maxCalculationTimeSec: Int, hasTimeOut: Boolean) {

    private val thread: Thread
    val shardId: Int
    val instant: Instant
    val maxCalculationTimeSec: Int
    private val hasTimeOut: Boolean

    init {
        this.thread = Thread.currentThread()
        this.shardId = shardId
        this.instant = Instant.now()
        this.maxCalculationTimeSec = maxCalculationTimeSec
        this.hasTimeOut = hasTimeOut
    }

    fun stop() {
        if (hasTimeOut) {
            thread.interrupt()
        }
    }
}