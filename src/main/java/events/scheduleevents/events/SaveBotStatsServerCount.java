package events.scheduleevents.events;

import core.Bot;
import core.DiscordApiManager;
import events.scheduleevents.ScheduleEventDaily;
import core.schedule.ScheduleInterface;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsServerCount implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 1) {
            DiscordApiManager.getInstance().getGlobalServerSize().ifPresent(DBBotStats::saveStatsServers);
        }
    }

}
