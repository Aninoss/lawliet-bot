package events.scheduleevents.events;

import java.util.ArrayList;
import java.util.Locale;
import constants.Category;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.Program;
import core.TextManager;
import core.schedule.ScheduleInterface;
import core.utils.JDAUtil;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import net.dv8tion.jda.api.EmbedBuilder;

@ScheduleEventDaily
public class ReminderDaily implements ScheduleInterface {

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
                        .setDescription(TextManager.getString(locale, Category.FISHERY, "daily_message_desc"))
                        .setFooter(TextManager.getString(locale, Category.FISHERY, "cooldowns_footer"));
                JDAUtil.sendPrivateMessage(sub.getUserId(), eb.build()).queue();
            }
        }
    }

}
