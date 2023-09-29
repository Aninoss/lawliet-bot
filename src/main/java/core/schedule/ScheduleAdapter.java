package core.schedule;

import constants.ExceptionRunnable;
import core.GlobalThreadPool;
import core.MainLogger;

public class ScheduleAdapter implements Runnable {

    private final ExceptionRunnable scheduleEvent;

    public ScheduleAdapter(ExceptionRunnable scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    @Override
    public void run() {
        GlobalThreadPool.submit(() -> {
            try {
                scheduleEvent.run();
            } catch (Throwable throwable) {
                MainLogger.get().error("Scheduled event failed", throwable);
            }
        });
    }

}
