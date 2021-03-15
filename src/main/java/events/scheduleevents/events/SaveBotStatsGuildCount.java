package events.scheduleevents.events;

import core.Program;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsGuildCount implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.isProductionMode() && Program.isPublicVersion() && Program.getClusterId() == 1) {
            ShardManager.getInstance().getGlobalGuildSize().ifPresent(DBBotStats::saveStatsServers);
        }
    }

}
