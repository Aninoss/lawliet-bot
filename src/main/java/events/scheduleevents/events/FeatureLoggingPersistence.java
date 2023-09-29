package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.Program;
import core.featurelogger.FeatureLogger;
import events.scheduleevents.ScheduleEventDaily;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@ScheduleEventDaily
public class FeatureLoggingPersistence implements ExceptionRunnable {

    @Override
    public void run() throws InterruptedException {
        TimeUnit.SECONDS.sleep(60 + Program.getClusterId());
        FeatureLogger.persist(LocalDate.now().minusDays(1));
    }

}
