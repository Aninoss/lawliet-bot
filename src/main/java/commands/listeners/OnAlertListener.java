package commands.listeners;

import constants.TrackerResult;
import mysql.modules.tracker.TrackerSlot;

public interface OnAlertListener {

    TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable;

    boolean trackerUsesKey();

}