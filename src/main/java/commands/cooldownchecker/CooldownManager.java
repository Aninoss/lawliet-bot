package commands.cooldownchecker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

public class CooldownManager {

    private static final CooldownManager ourInstance = new CooldownManager();
    public static CooldownManager getInstance() {
        return ourInstance;
    }
    private CooldownManager() {}

    private final LoadingCache<Long, CooldownUserData> cooldownUserDataMap = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public CooldownUserData load(@NonNull Long userId) throws Exception {
                    return new CooldownUserData();
                }
            });

    public synchronized CooldownUserData getCooldownData(long userId) {
        return cooldownUserDataMap.getUnchecked(userId);
    }

}