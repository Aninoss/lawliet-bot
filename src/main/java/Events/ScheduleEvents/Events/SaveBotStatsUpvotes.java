package Events.ScheduleEvents.Events;

import Core.Bot;
import Events.ScheduleEvents.ScheduleEventDaily;
import Events.ScheduleEvents.ScheduleEventInterface;
import MySQL.Modules.BotStats.DBBotStats;

@ScheduleEventDaily
public class SaveBotStatsUpvotes implements ScheduleEventInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            DBBotStats.saveStatsUpvotes();
        }
    }

}
