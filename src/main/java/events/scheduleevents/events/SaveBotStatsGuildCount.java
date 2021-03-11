package events.scheduleevents.events;

import core.Bot;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsGuildCount implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 1) {
            ShardManager.getInstance().getGlobalGuildSize().ifPresent(DBBotStats::saveStatsServers);
        }
    }

}
