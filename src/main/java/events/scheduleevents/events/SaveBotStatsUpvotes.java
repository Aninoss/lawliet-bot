package events.scheduleevents.events;

import core.Bot;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.botstats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsUpvotes implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 1) {
            DBBotStats.saveStatsUpvotes();
        }
    }

}
