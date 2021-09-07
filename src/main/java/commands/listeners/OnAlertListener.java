package commands.listeners;

import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;

public interface OnAlertListener {

    AlertResponse onTrackerRequest(TrackerData slot) throws Throwable;

    boolean trackerUsesKey();

}