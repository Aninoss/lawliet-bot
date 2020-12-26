package events.scheduleevents.events;

import constants.Settings;
import core.Bot;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventHourly;
import mysql.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Calendar;

@ScheduleEventHourly
public class MySQLCleanCache implements ScheduleInterface {

    final Logger LOGGER = LoggerFactory.getLogger(MySQLCleanCache.class);

    private boolean ready = false;

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            if (hour == Settings.RESTART_HOUR && ready) {
                ready = false;
                DBMain.getInstance().clearCache();
                LOGGER.info("Cache cleaned");
            } else if (hour < Settings.RESTART_HOUR) {
                ready = true;
            }
        }
    }

}
