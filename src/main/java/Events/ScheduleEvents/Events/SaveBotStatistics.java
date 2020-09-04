package Events.ScheduleEvents.Events;

import Core.Bot;
import Core.DiscordApiCollection;
import Events.ScheduleEvents.ScheduleEventDaily;
import Events.ScheduleEvents.ScheduleEventInterface;
import MySQL.DBBotStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ScheduleEventDaily
public class SaveBotStatistics implements ScheduleEventInterface {

    final Logger LOGGER = LoggerFactory.getLogger(SaveBotStatistics.class);

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            try {
                DBBotStats.addStatCommandUsages();
            } catch (Exception e) {
                LOGGER.error("Could not post command usages stats", e);
            }
            try {
                DBBotStats.addStatServers(DiscordApiCollection.getInstance().getServerTotalSize());
            } catch (Exception e) {
                LOGGER.error("Could not post total server count", e);
            }
            try {
                DBBotStats.addStatUpvotes();
            } catch (Exception e) {
                LOGGER.error("Could not post upvotes stats", e);
            }
        }
    }

}
