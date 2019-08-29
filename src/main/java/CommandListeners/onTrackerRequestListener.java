package CommandListeners;

import General.Tracker.TrackerData;

public interface onTrackerRequestListener {
    TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable;
    boolean trackerUsesKey();
    boolean needsPrefix();
}