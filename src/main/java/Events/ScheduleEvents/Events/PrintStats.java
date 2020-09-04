package Events.ScheduleEvents.Events;

import Core.Console;
import Events.ScheduleEvents.ScheduleEventFixedRate;
import Events.ScheduleEvents.ScheduleEventInterface;
import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class PrintStats implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        System.out.println(Console.getInstance().getStats());
    }

}