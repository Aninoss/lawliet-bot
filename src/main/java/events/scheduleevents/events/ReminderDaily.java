package events.scheduleevents.events;

import commands.Category;
import constants.ExceptionRunnable;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.Program;
import core.TextManager;
import events.scheduleevents.ScheduleEventDaily;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.ArrayList;
import java.util.Locale;

@ScheduleEventDaily
public class ReminderDaily implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        if (Program.isMainCluster()) {
            CustomObservableMap<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.DAILY);
            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
                for (SubSlot sub : new ArrayList<>(subMap.values())) {
                    Locale locale = sub.getLocale();
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, Category.FISHERY, "daily_message_title"))
                            .setDescription(TextManager.getString(locale, Category.FISHERY, "daily_message_desc"));

                    sub.sendEmbed(entityManager, locale, eb);
                }
            }
        }
    }

}
