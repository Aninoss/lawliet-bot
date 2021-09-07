package events.scheduleevents.events;

import constants.ExceptionRunnable;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearOldUpvotes implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        DBUpvotes.getInstance().cleanUp();
    }

}
