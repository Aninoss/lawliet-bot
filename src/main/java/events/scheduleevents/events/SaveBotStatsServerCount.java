package events.scheduleevents.events;

import core.Bot;
import core.DiscordApiCollection;
import events.scheduleevents.ScheduleEventDaily;
import events.scheduleevents.ScheduleEventInterface;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsServerCount implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            DBBotStats.saveStatsServers(DiscordApiCollection.getInstance().getServerTotalSize());
        }
    }

}
