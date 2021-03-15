package events.scheduleevents.events;

import core.Program;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsCommandUsages implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.isProductionMode() && Program.isPublicVersion() && Program.getClusterId() == 1) {
            DBBotStats.saveStatsCommandUsages();
        }
    }

}
