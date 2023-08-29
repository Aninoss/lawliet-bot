package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import events.scheduleevents.ScheduleEventFixedRate;
import events.sync.SendEvent;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class CheckSubscriptionActive implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.publicVersion()) {
            return;
        }

        long subId = Long.parseLong(System.getenv("SUB_ID"));
        try {
            if (subId != -1 && !SendEvent.sendSubscriptionActive(subId).get(5, TimeUnit.SECONDS)) {
                MainLogger.get().info("EXIT - Subscription not active anymore");
                System.exit(8);
            } else {
                MainLogger.get().info("Subscription check passed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            MainLogger.get().error("Subscription retrieval error", e);
        }
    }

}
