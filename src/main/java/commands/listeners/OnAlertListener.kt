package commands.listeners

import modules.schedulers.AlertResponse
import mysql.modules.tracker.TrackerData

interface OnAlertListener {

    @Throws(Throwable::class)
    fun onTrackerRequest(slot: TrackerData): AlertResponse

    fun trackerUsesKey(): Boolean

}