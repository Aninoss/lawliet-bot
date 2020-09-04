package Events.ScheduleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class ScheduleEventAdapter extends TimerTask {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleEventAdapter.class);

    private final ScheduleEventInterface scheduleEvent;

    public ScheduleEventAdapter(ScheduleEventInterface scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    @Override
    public void run() {
        try {
            scheduleEvent.run();
        } catch (Throwable throwable) {
            LOGGER.error("Scheduled event failed", throwable);
        }
    }

}
