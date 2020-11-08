package events.scheduleevents.events;

import constants.Settings;
import core.Bot;
import core.utils.SystemUtil;
import events.scheduleevents.ScheduleEventHourly;
import core.schedule.ScheduleInterface;
import mysql.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

@ScheduleEventHourly
public class BotRestart implements ScheduleInterface {

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

    private void backupDatabase() {
        LOGGER.info("Backup database...");
        try {
            SystemUtil.backupDB();
        } catch (Exception e) {
            LOGGER.error("Error while creating database backup", e);
        }
    }

}
