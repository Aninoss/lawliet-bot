package mysql.hibernate;

import core.MainLogger;
import mysql.hibernate.entity.GuildEntity;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public class HibernateManager {

    private static EntityManagerFactory entityManagerFactory;

    public static void connect() {
        Map<String, Object> configOverrides = new HashMap<>();
        addConfigOverride(configOverrides, "hibernate.ogm.mongodb.host", "MONGODB_HOST");
        addConfigOverride(configOverrides, "hibernate.ogm.mongodb.port", "MONGODB_PORT");
        addConfigOverride(configOverrides, "hibernate.ogm.mongodb.username", "MONGODB_USER");
        addConfigOverride(configOverrides, "hibernate.ogm.mongodb.username", "MONGODB_PASSWORD");
        MainLogger.get().info("Connecting with MongoDB database");
        entityManagerFactory = Persistence.createEntityManagerFactory("lawliet", configOverrides);
    }

    public static EntityManagerWrapper createEntityManager() {
        return new EntityManagerWrapper(entityManagerFactory.createEntityManager());
    }

    public static GuildEntity findGuildEntity(long guildId) {
        return createEntityManager().findGuildEntity(guildId);
    }

    private static void addConfigOverride(Map<String, Object> configOverrides, String configKey, String environmentKey) {
        String value = System.getenv(environmentKey);
        if (value != null) {
            configOverrides.put(configKey, value);
        }
    }

}
