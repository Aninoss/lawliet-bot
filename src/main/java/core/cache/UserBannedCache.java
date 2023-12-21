package core.cache;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

public class UserBannedCache extends SingleCache<Set<Long>> {

    private static final UserBannedCache ourInstance = new UserBannedCache();

    public static UserBannedCache getInstance() {
        return ourInstance;
    }

    private UserBannedCache() {
    }

    @Override
    protected Set<Long> fetchValue() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
            return entityManager.createQuery("FROM " + UserEntity.class.getName() + " WHERE banReason IS NOT NULL", UserEntity.class)
                    .getResultList()
                    .stream()
                    .map(UserEntity::getUserId)
                    .collect(Collectors.toSet());
        }
    }

    public boolean isBanned(long userId) {
        return getAsync().contains(userId);
    }
}
