package commands.listeners;

import constants.TrackerResult;
import mysql.modules.tracker.TrackerData;

public interface OnAlertListener {

    TrackerResult onTrackerRequest(TrackerData slot) throws Throwable;

    boolean trackerUsesKey();

}