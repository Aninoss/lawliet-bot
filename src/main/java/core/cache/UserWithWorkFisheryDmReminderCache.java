package core.cache;

import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.hibernate.entity.user.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

public class UserWithWorkFisheryDmReminderCache extends SingleCache<Set<Long>> {

    private static final UserWithWorkFisheryDmReminderCache ourInstance = new UserWithWorkFisheryDmReminderCache();

    public static UserWithWorkFisheryDmReminderCache getInstance() {
        return ourInstance;
    }

    private UserWithWorkFisheryDmReminderCache() {
    }

    @Override
    protected Set<Long> fetchValue() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(UserWithWorkFisheryDmReminderCache.class)) {
            return FisheryDmReminderEntity.findAllUserEntitiesWithType(entityManager, FisheryDmReminderEntity.Type.WORK)
                    .stream()
                    .map(UserEntity::getUserId)
                    .collect(Collectors.toSet());
        }
    }

    public boolean hasWorkFisheryDmReminder(long userId) {
        return getAsync().contains(userId);
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 1;
    }

}
