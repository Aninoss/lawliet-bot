package events.scheduleevents.events;

import java.util.ArrayList;
import java.util.Locale;
import commands.Category;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.Program;
import core.TextManager;
import constants.ExceptionRunnable;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import net.dv8tion.jda.api.EmbedBuilder;

@ScheduleEventDaily
public class ReminderDaily implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        if (Program.getClusterId() == 1) {
            CustomObservableMap<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.DAILY);
            for (SubSlot sub : new ArrayList<>(subMap.values())) {
                Locale locale = sub.getLocale();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(locale, Category.FISHERY, "daily_message_title"))
                        .setDescription(TextManager.getString(locale, Category.FISHERY, "daily_message_desc"));
                sub.sendEmbed(locale, eb);
            }
        }
    }

}
