package mysql.hibernate;

import core.MainLogger;
import mysql.hibernate.entity.GuildEntity;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.util.Properties;

public class HibernateManager {

    private static EntityManagerFactory entityManagerFactory;

    public static void connect() throws IOException {
        Properties props = new Properties();
        props.put("hibernate.ogm.datastore.host", System.getenv("MONGODB_HOST"));
        props.put("hibernate.ogm.datastore.username", System.getenv("MONGODB_USER"));
        props.put("hibernate.ogm.datastore.password", System.getenv("MONGODB_PASSWORD"));

        MainLogger.get().info("Connecting with MongoDB database");
        entityManagerFactory = Persistence.createEntityManagerFactory("lawliet", props);
    }

    public static EntityManagerWrapper createEntityManager() {
        return new EntityManagerWrapper(entityManagerFactory.createEntityManager());
    }

    public static GuildEntity findGuildEntity(long guildId) {
        return createEntityManager().findGuildEntity(guildId);
    }

}
