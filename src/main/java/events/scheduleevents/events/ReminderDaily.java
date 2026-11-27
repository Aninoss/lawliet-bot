package events.scheduleevents.events;

import commands.Category;
import constants.ExceptionRunnable;
import core.Program;
import core.TextManager;
import events.scheduleevents.ScheduleEventDaily;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.hibernate.entity.user.UserEntity;

import java.util.List;
import java.util.Locale;

@ScheduleEventDaily
public class ReminderDaily implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        if (!Program.isMainCluster()) {
            return;
        }

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(ReminderDaily.class)) {
            List<UserEntity> userEntities = FisheryDmReminderEntity.findAllUserEntitiesWithType(entityManager, FisheryDmReminderEntity.Type.DAILY);
            for (UserEntity userEntity : userEntities) {
                FisheryDmReminderEntity fisheryDmReminder = userEntity.getFisheryDmReminders().get(FisheryDmReminderEntity.Type.DAILY);
                Locale locale = fisheryDmReminder.getLocale();

                fisheryDmReminder.sendReminder(
                        TextManager.getString(locale, Category.FISHERY, "daily_message_title"),
                        TextManager.getString(locale, Category.FISHERY, "daily_message_desc")
                );
            }
        }
    }

}
