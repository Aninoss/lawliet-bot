package events.scheduleevents.events;

import commands.runningchecker.RunningCheckerManager;
import core.cache.ServerPatreonBoostCache;
import events.scheduleevents.ScheduleEventDaily;
import core.schedule.ScheduleInterface;
import modules.porn.PornImageCache;
import modules.reddit.SubredditContainer;
import mysql.modules.upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearOldUpvotes implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        DBUpvotes.getInstance().cleanUp();
    }

}
