package Events.ScheduleEvents.Events;

import CommandSupporters.Cooldown.Cooldown;
import Events.ScheduleEvents.ScheduleEventFixedRate;
import Events.ScheduleEvents.ScheduleEventInterface;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 5, rateUnit = ChronoUnit.MINUTES)
public class CleanCooldownList implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        Cooldown.getInstance().clean();
    }

}