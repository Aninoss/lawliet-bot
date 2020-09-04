package Events.ScheduleEvents.Events;

import Core.Bot;
import Core.DiscordApiCollection;
import Events.ScheduleEvents.ScheduleEventDaily;
import Events.ScheduleEvents.ScheduleEventInterface;
import MySQL.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsServerCount implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            DBBotStats.saveStatsServers(DiscordApiCollection.getInstance().getServerTotalSize());
        }
    }

}
