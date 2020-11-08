package events.scheduleevents.events;

import commands.cooldownchecker.CooldownManager;
import events.scheduleevents.ScheduleEventFixedRate;
import core.schedule.ScheduleInterface;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 5, rateUnit = ChronoUnit.MINUTES)
public class CleanCooldownList implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        CooldownManager.getInstance().clean();
    }

}