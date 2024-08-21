package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import events.scheduleevents.ScheduleEventFixedRate;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ScheduleEventFixedRate(rateUnit = ChronoUnit.DAYS, rateValue = 1)
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
        ArrayList<File> filesToBeDeleted = new ArrayList<>();
        int keptFiles = 0;

        for (File file : new File(System.getenv("ROOT_DIR") + "/data/cdn/" + dirName).listFiles()) {
            if (entityManager.findAllWithValue(entityClass, fieldName, file.getName()).isEmpty() && checkFileTime(file)) {
                filesToBeDeleted.add(file);
            } else {
                keptFiles++;
            }
        }

        if (keptFiles >= filesToBeDeleted.size() * 3) {
            filesToBeDeleted.forEach(File::delete);
            MainLogger.get().info("{} {} images deleted", filesToBeDeleted.size(), dirName);
        } else {
            MainLogger.get().error("{} images could not be deleted (delete: {}; keep: {})", dirName, filesToBeDeleted.size(), keptFiles);
        }
    }

    private static void executeCustomCommands(EntityManagerWrapper entityManager) {
        ArrayList<File> filesToBeDeleted = new ArrayList<>();
        int keptFiles = 0;

        Set<String> customCommandFilenames = (Set<String>) entityManager.createNativeQuery("{'customCommands': {$exists: true}}", GuildEntity.class).getResultList().stream()
                .flatMap(guildEntity -> ((GuildEntity) guildEntity).getCustomCommands().values().stream().map(CustomCommandEntity::getImageFilename))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (File file : new File(System.getenv("ROOT_DIR") + "/data/cdn/custom").listFiles()) {
            if (!customCommandFilenames.contains(file.getName()) && checkFileTime(file)) {
                filesToBeDeleted.add(file);
            } else {
                keptFiles++;
            }
        }

        if (keptFiles >= filesToBeDeleted.size() * 3) {
            filesToBeDeleted.forEach(File::delete);
            MainLogger.get().info("{} custom images deleted", filesToBeDeleted.size());
        } else {
            MainLogger.get().error("custom images could not be deleted (delete: {}; keep: {})", filesToBeDeleted.size(), keptFiles);
        }
    }

    private static void executeCustomRoleplay(EntityManagerWrapper entityManager) {
        ArrayList<File> filesToBeDeleted = new ArrayList<>();
        int keptFiles = 0;

        for (File file : new File(System.getenv("ROOT_DIR") + "/data/cdn/customrp").listFiles()) {
            String query = "{'imageFilenames': ':filename'}".replace(":filename", file.getName());
            if (entityManager.createNativeQuery(query, CustomRolePlayEntity.class).getResultList().isEmpty() && checkFileTime(file)) {
                filesToBeDeleted.add(file);
            } else {
                keptFiles++;
            }
        }

        if (keptFiles >= filesToBeDeleted.size() * 3) {
            filesToBeDeleted.forEach(File::delete);
            MainLogger.get().info("{} customrp images deleted", filesToBeDeleted.size());
        } else {
            MainLogger.get().error("customrp images could not be deleted (delete: {}; keep: {})", filesToBeDeleted.size(), keptFiles);
        }
    }

    private static boolean checkFileTime(File file) {
        try {
            FileTime creationTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
            return Instant.now().minus(Duration.ofHours(1)).isAfter(creationTime.toInstant());
        } catch (IOException e) {
            MainLogger.get().error("Image cleaner creation time exception", e);
            return false;
        }
    }

}
