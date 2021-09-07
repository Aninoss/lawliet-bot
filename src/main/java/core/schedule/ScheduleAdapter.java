package core.schedule;

import constants.ExceptionRunnable;
import core.MainLogger;

public class ScheduleAdapter implements Runnable {

    private final ExceptionRunnable scheduleEvent;

    public ScheduleAdapter(ExceptionRunnable scheduleEvent) {
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
