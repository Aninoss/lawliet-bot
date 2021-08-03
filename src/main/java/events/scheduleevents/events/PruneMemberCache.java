package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import core.MainLogger;
import core.MemberCacheController;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class PruneMemberCache implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        int prunedMembers = MemberCacheController.getInstance().pruneAll();
        MainLogger.get().info("{} members pruned from cache", prunedMembers);
    }

}