package Events.ScheduleEvents.Events;

import Core.DiscordConnector;
import Events.ScheduleEvents.ScheduleEventFixedRate;
import Events.ScheduleEvents.ScheduleEventInterface;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class UpdateBotActivity implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        DiscordConnector.getInstance().updateActivity();
    }

}