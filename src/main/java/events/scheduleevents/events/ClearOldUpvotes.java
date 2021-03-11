package events.scheduleevents.events;

import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearOldUpvotes implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        DBUpvotes.getInstance().cleanUp();
    }

}
