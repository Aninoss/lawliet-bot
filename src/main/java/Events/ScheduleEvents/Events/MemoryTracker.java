package Events.ScheduleEvents.Events;

import Core.Console;
import Events.ScheduleEvents.ScheduleEventFixedRate;
import Events.ScheduleEvents.ScheduleEventInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 500, rateUnit = ChronoUnit.MILLIS)
public class MemoryTracker implements ScheduleEventInterface {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryTracker.class);

    @Override
    public void run() throws Throwable {
        double maxMemory = Console.getInstance().getMaxMemory();
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        if (memoryUsed > maxMemory) {
            maxMemory = memoryUsed;
            Console.getInstance().setMaxMemory(maxMemory);

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Max Memory: {} / {}", memoryUsed, memoryTotal);
        }
    }

}