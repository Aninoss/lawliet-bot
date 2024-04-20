package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.entities.Guild;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.DAYS)
public class CleanGuilds implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() throws InterruptedException {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(CleanGuilds.class)) {
            MainLogger.get().info("Starting guild cleaner");
            long counter = updateGuilds(entityManager);
            MainLogger.get().info("{} guild dates have been updated", counter);

            if (!ShardManager.isEverythingConnected()) {
                MainLogger.get().error("Guild cleaner stopped due to missing connections");
                return;
            }

            //TODO: Implement cleanup
        }
    }

    private static long updateGuilds(EntityManagerWrapper entityManager) {
        long counter = 0;
        for (Guild guild : ShardManager.getLocalGuilds()) {
            GuildEntity guildEntity = entityManager.findGuildEntity(guild.getIdLong());
            if (guildEntity.getLatestPresentDate() == null || !guildEntity.getLatestPresentDate().equals(LocalDate.now())) {
                guildEntity.beginTransaction();
                guildEntity.setLatestPresentDate(LocalDate.now());
                guildEntity.commitTransaction();
                counter++;
            }
            entityManager.clear();
        }
        return counter;
    }

}
