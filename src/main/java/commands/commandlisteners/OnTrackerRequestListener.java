package commands.commandlisteners;

import constants.TrackerResult;
import mysql.modules.tracker.TrackerBeanSlot;

public interface OnTrackerRequestListener {

    TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable;
    boolean trackerUsesKey();

}