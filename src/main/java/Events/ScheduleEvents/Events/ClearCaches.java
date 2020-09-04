package Events.ScheduleEvents.Events;

import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.PatreonCache;
import Core.ServerPatreonBoostCache;
import Events.ScheduleEvents.ScheduleEventDaily;
import Events.ScheduleEvents.ScheduleEventInterface;
import Modules.Porn.PornImageCache;
import Modules.Reddit.SubredditContainer;
import MySQL.Modules.Upvotes.DBUpvotes;

@ScheduleEventDaily
public class ClearCaches implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        PatreonCache.getInstance().reset();
        SubredditContainer.getInstance().reset();
        RunningCommandManager.getInstance().clear();
        PornImageCache.getInstance().reset();
        DBUpvotes.getInstance().cleanUp();
        ServerPatreonBoostCache.getInstance().reset();
        PatreonCache.getInstance().reset();
    }

}
