package core.featurelogger;

import core.MainLogger;
import core.ShardManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.FeatureLoggingEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FeatureLogger {

    private final static long MILLIS_PER_HOUR = 1000L * 60L * 60L;
    private final static HashMap<LocalDate, HashMap<PremiumFeature, HashSet<Long>>> mapPremium = new HashMap<>();

    private static long startTime = 0;

    public static void start() {
        startTime = System.currentTimeMillis();
    }

    public static synchronized void inc(PremiumFeature premiumFeature, long guildId) {
        mapPremium.computeIfAbsent(LocalDate.now(), k -> new HashMap<>())
                .computeIfAbsent(premiumFeature, k -> new HashSet<>())
                .add(guildId);
    }

    public static void persist(LocalDate date) {
        if (startTime == 0) {
            return;
        }

        long guildHours = Math.round(Math.min(24, getHoursAfterStartTime()) * ShardManager.getLocalGuildSize().get());
        HashMap<PremiumFeature, HashSet<Long>> dateMapPremium = mapPremium.getOrDefault(date, new HashMap<>());

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(FeatureLogger.class)) {
            entityManager.getTransaction().begin();
            FeatureLoggingEntity featureLoggingEntity = entityManager.find(FeatureLoggingEntity.class, date);

            if (featureLoggingEntity == null) {
                FeatureLoggingEntity newFeatureLoggingEntity = new FeatureLoggingEntity(date);
                dateMapPremium.forEach((premiumFeature, guildIds) -> newFeatureLoggingEntity.getFeatures().put(premiumFeature, guildIds.size()));
                newFeatureLoggingEntity.setGuildHours(guildHours);
                entityManager.persist(newFeatureLoggingEntity);
            } else {
                Map<PremiumFeature, Integer> features = featureLoggingEntity.getFeatures();
                for (PremiumFeature premiumFeature : dateMapPremium.keySet()) {
                    int n = features.getOrDefault(premiumFeature, 0);
                    features.put(premiumFeature, n + dateMapPremium.get(premiumFeature).size());
                }
                featureLoggingEntity.setGuildHours(featureLoggingEntity.getGuildHours() + guildHours);
            }

            entityManager.getTransaction().commit();
        }

        MainLogger.get().info("Added {} guild hours to feature logger", guildHours);
        mapPremium.remove(date);
    }

    private static double getHoursAfterStartTime() {
        return (System.currentTimeMillis() - startTime) / (double) MILLIS_PER_HOUR;
    }

}
