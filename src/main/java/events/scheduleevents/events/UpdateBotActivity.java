package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import constants.ExceptionRunnable;
import core.JDAWrapper;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;

@ScheduleEventFixedRate(rateValue = 60, rateUnit = ChronoUnit.MINUTES)
public class UpdateBotActivity implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.publicVersion()) {
            ShardManager.getConnectedLocalJDAWrappers()
                    .forEach(JDAWrapper::updateActivity);
        }
    }

}