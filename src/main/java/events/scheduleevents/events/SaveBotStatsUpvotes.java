package events.scheduleevents.events;

import core.Bot;
import events.scheduleevents.ScheduleEventDaily;
import core.schedule.ScheduleInterface;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsUpvotes implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 0) {
            DBBotStats.saveStatsUpvotes();
        }
    }

}
