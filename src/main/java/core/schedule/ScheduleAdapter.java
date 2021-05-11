package core.schedule;

import core.MainLogger;

public class ScheduleAdapter implements Runnable {

    private final ScheduleInterface scheduleEvent;

    public ScheduleAdapter(ScheduleInterface scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    @Override
    public void run() {
        try {
            scheduleEvent.run();
        } catch (Throwable throwable) {
            MainLogger.get().error("Scheduled event failed", throwable);
        }
    }

}
