package events.scheduleevents.events;

import commands.commandcooldown.Cooldown;
import events.scheduleevents.ScheduleEventFixedRate;
import events.scheduleevents.ScheduleEventInterface;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 5, rateUnit = ChronoUnit.MINUTES)
public class CleanCooldownList implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        Cooldown.getInstance().clean();
    }

}