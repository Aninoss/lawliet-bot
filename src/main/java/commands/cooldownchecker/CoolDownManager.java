package commands.cooldownchecker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import constants.Settings;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

public class CoolDownManager {

    private static final CoolDownManager ourInstance = new CoolDownManager();

    public static CoolDownManager getInstance() {
        return ourInstance;
    }

    private CoolDownManager() {
    }

    private final LoadingCache<Long, CoolDownUserData> coolDownUserDataMap = CacheBuilder.newBuilder()
            .expireAfterAccess(Settings.COOLDOWN_TIME_SEC, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public CoolDownUserData load(@NonNull Long userId) {
                    return new CoolDownUserData();
                }
            });

    public synchronized CoolDownUserData getCoolDownData(long userId) {
        return coolDownUserDataMap.getUnchecked(userId);
    }

}