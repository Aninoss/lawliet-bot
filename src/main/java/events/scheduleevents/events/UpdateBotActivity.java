package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import core.DiscordConnector;
import core.ShardManager;
import constants.ExceptionRunnable;
import events.scheduleevents.ScheduleEventFixedRate;

@ScheduleEventFixedRate(rateValue = 60, rateUnit = ChronoUnit.MINUTES)
public class UpdateBotActivity implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        ShardManager.getConnectedLocalJDAs()
                .forEach(DiscordConnector::updateActivity);
    }

}