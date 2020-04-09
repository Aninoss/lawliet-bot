package CommandListeners;

import Constants.TrackerResult;
import MySQL.Modules.Tracker.TrackerBeanSlot;

public interface OnTrackerRequestListener {

    TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable;
    boolean trackerUsesKey();

}