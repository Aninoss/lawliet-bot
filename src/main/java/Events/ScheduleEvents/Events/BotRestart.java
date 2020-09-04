package Events.ScheduleEvents.Events;

import Constants.Settings;
import Core.Bot;
import Core.DiscordApiCollection;
import Core.Utils.SystemUtil;
import Events.ScheduleEvents.ScheduleEventHourly;
import Events.ScheduleEvents.ScheduleEventInterface;
import MySQL.DBBotStats;
import MySQL.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

@ScheduleEventHourly
public class BotRestart implements ScheduleEventInterface {

    final Logger LOGGER = LoggerFactory.getLogger(BotRestart.class);

    private boolean readyForRestart = false;

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            if (hour == Settings.UPDATE_HOUR && readyForRestart) {
                readyForRestart = false;
                backupDatabase();
                saveDailyUniqueUsersStats();

                if (Bot.hasUpdate()) {
                    LOGGER.info("EXIT - Restarting for update...");
                    System.exit(0);
                } else {
                    DBMain.getInstance().clearCache();
                    LOGGER.info("Cache cleaned successfully");
                }
            } else if (hour < Settings.UPDATE_HOUR) {
                readyForRestart = true;
            }
        }
    }

    private void saveDailyUniqueUsersStats() {
        if (DiscordApiCollection.getInstance().getStartingTime().isBefore(Instant.now().minus(23, ChronoUnit.HOURS))) {
            try {
                DBBotStats.addStatUniqueUsers();
            } catch (Exception e) {
                LOGGER.error("Could not post unique users stats", e);
            }
        }
    }

    private void backupDatabase() {
        LOGGER.info("Backup database...");
        try {
            SystemUtil.backupDB();
        } catch (Exception e) {
            LOGGER.error("Error while creating database backup", e);
        }
    }

}
