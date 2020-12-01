package events.scheduleevents.events;

import core.Bot;
import core.Console;
import events.scheduleevents.ScheduleEventFixedRate;
import core.schedule.ScheduleInterface;
import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class PrintStats implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode())
            System.out.println(Console.getInstance().getStats());
    }

}