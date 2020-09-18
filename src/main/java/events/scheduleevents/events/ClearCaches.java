package events.scheduleevents.events;

import commands.commandrunningchecker.RunningCheckerManager;
import core.PatreonCache;
import core.ServerPatreonBoostCache;
import events.scheduleevents.ScheduleEventDaily;
import events.scheduleevents.ScheduleEventInterface;
import modules.porn.PornImageCache;
import modules.reddit.SubredditContainer;
import mysql.modules.upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearCaches implements ScheduleEventInterface {

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
