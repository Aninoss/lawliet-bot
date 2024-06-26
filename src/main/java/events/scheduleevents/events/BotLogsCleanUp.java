package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.BotLogEntity;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class BotLogsCleanUp implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode() && Program.publicInstance() && Program.isMainCluster()) {
            execute();
        }
    }

    public static void execute() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(BotLogsCleanUp.class)) {
            entityManager.getTransaction().begin();
            int deleted = BotLogEntity.cleanUp(entityManager);
            entityManager.getTransaction().commit();
            MainLogger.get().info("Bot logs clean up completed ({} documents deleted)", deleted);
        }
    }

}
