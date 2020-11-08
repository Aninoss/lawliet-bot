package events.scheduleevents.events;

import core.DiscordConnector;
import events.scheduleevents.ScheduleEventFixedRate;
import core.schedule.ScheduleInterface;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class UpdateBotActivity implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        DiscordConnector.getInstance().updateActivity();
    }

}