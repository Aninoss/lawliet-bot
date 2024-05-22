package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import events.scheduleevents.ScheduleEventDaily;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.CustomRolePlayEntity;
import mysql.hibernate.entity.GiveawayEntity;
import mysql.hibernate.entity.ReactionRoleEntity;
import mysql.hibernate.entity.guild.CustomCommandEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.template.HibernateEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ScheduleEventDaily
public class CleanImages implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        if (!Program.isMainCluster() || !Program.publicInstance()) {
            return;
        }

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(CleanImages.class)) {
            executeSimple(entityManager, ReactionRoleEntity.class, "reactionroles", "imageFilename");
            executeSimple(entityManager, GiveawayEntity.class, "giveaway", "imageFilename");
            executeCustomCommands(entityManager);
            executeCustomRoleplay(entityManager);
        }
    }

    private static void executeSimple(EntityManagerWrapper entityManager, Class<? extends HibernateEntity> entityClass, String dirName, String fieldName) {
        int removed = 0;
        File[] files = new File(System.getenv("ROOT_DIR") + "/data/cdn/" + dirName).listFiles();
        for (File file : files) {
            if (entityManager.findAllWithValue(entityClass, fieldName, file.getName()).isEmpty() && deleteFile(file)) {
                removed += 1;
            }
        }

        MainLogger.get().info("{} {} images removed", removed, dirName);
    }

    private static void executeCustomCommands(EntityManagerWrapper entityManager) {
        Set<String> customCommandFilenames = (Set<String>) entityManager.createNativeQuery("{'customCommands': {$exists: true}}", GuildEntity.class).getResultList().stream()
                .flatMap(guildEntity -> ((GuildEntity) guildEntity).getCustomCommands().values().stream().map(CustomCommandEntity::getImageFilename))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int removed = 0;
        File[] files = new File(System.getenv("ROOT_DIR") + "/data/cdn/custom").listFiles();
        for (File file : files) {
            if (!customCommandFilenames.contains(file.getName()) && deleteFile(file)) {
                removed += 1;
            }
        }

        MainLogger.get().info("{} custom images removed", removed);

    }

    private static void executeCustomRoleplay(EntityManagerWrapper entityManager) {
        int removed = 0;
        File[] files = new File(System.getenv("ROOT_DIR") + "/data/cdn/customrp").listFiles();
        for (File file : files) {
            String query = "{'imageFilenames': ':filename'}".replace(":filename", file.getName());
            if (entityManager.createNativeQuery(query, CustomRolePlayEntity.class).getResultList().isEmpty() && deleteFile(file)) {
                removed += 1;
            }
        }

        MainLogger.get().info("{} customrp images removed", removed);
    }

    private static boolean deleteFile(File file) {
        try {
            FileTime creationTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
            if (Instant.now().minus(Duration.ofHours(1)).isBefore(creationTime.toInstant())) {
                return false;
            }
        } catch (IOException e) {
            MainLogger.get().error("Image cleaner creation time exception", e);
            return false;
        }

        return file.delete();
    }

}
