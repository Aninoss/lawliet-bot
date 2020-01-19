package General.Tracker;

public class TrackerConnection {

    private TrackerData trackerData;
    private Thread thread;

    public TrackerConnection(TrackerData trackerData, Thread thread) {
        this.trackerData = trackerData;
        this.thread = thread;
    }

    public TrackerData getTrackerData() {
        return trackerData;
    }

    public Thread getThread() {
        return thread;
    }
}
