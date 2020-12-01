package events.scheduleevents.events;

import commands.runningchecker.RunningCheckerManager;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import events.scheduleevents.ScheduleEventDaily;
import core.schedule.ScheduleInterface;
import modules.porn.PornImageCache;
import modules.reddit.SubredditContainer;
import mysql.modules.upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearCaches implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        PatreonCache.getInstance().reset();
        SubredditContainer.getInstance().reset();
        RunningCheckerManager.getInstance().clear();
        PornImageCache.getInstance().reset();
        DBUpvotes.getInstance().cleanUp();
        ServerPatreonBoostCache.getInstance().reset();
    }

}
