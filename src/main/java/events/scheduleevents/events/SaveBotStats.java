package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStats implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode() && Program.publicInstance() && Program.isMainCluster()) {
            ShardManager.getGlobalGuildSize().ifPresent(DBBotStats::saveStatsServers);
            DBBotStats.saveStatsCommandUsages();
            DBBotStats.saveStatsUpvotes();
        }
    }

}
