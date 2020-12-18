package events.scheduleevents.events;

import core.DiscordConnector;
import core.DiscordApiManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class UpdateBotActivity implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        DiscordApiManager.getInstance().getConnectedLocalApis().forEach(api -> DiscordConnector.getInstance().updateActivity(api));
    }

}