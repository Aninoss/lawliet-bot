package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.LocalFile;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
            if (!Program.publicInstance()) {
                return;
            }

            String queryString = """
                    {
                      $and: [
                        { $expr: { $gte: [{ $mod: [{ $floor: { $divide: [{ $toLong: "$_id"}, :divisor] }}, :totalShards] }, :shardIntervalMin] } },
                        { $expr: { $lte: [{ $mod: [{ $floor: { $divide: [{ $toLong: "$_id"}, :divisor] }}, :totalShards] }, :shardIntervalMax] } },
                        { $or: [
                          { latestPresentDate: { $lt: ":date" } },
                          { latestPresentDate: null }
                        ] }
                      ]
                    }
                    """.replace(":divisor", String.valueOf((long) Math.pow(2, 22)))
                    .replace(":totalShards", String.valueOf(ShardManager.getTotalShards()))
                    .replace(":shardIntervalMin", String.valueOf(ShardManager.getShardIntervalMin()))
                    .replace(":shardIntervalMax", String.valueOf(ShardManager.getShardIntervalMax()))
                    .replace(":date", LocalDate.now().minusDays(30).toString());

            List<GuildEntity> guildEntityList = entityManager.createNativeQuery(queryString, GuildEntity.class).getResultList();
            for (GuildEntity guildEntity : guildEntityList) {
                guildEntity.setEntityManager(entityManager);

                LocalFile welcomeBackgroundFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", guildEntity.getGuildId()));
                if (welcomeBackgroundFile.exists()) {
                    welcomeBackgroundFile.delete();
                }
                guildEntity.getWelcomeMessages().getJoin().getImageFiles().forEach(File::delete);
                guildEntity.getWelcomeMessages().getDm().getImageFiles().forEach(File::delete);
                guildEntity.getWelcomeMessages().getLeave().getImageFiles().forEach(File::delete);
                if (guildEntity.getFishery().getFisheryStatus() != FisheryStatus.STOPPED) {
                    FisheryUserManager.deleteGuildData(guildEntity.getGuildId());
                }

                entityManager.getTransaction().begin();
                entityManager.remove(guildEntity);
                entityManager.getTransaction().commit();
            }

            MainLogger.get().info("{} guild entries have been removed", guildEntityList.size());
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
