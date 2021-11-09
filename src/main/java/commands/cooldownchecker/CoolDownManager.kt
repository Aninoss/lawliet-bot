package commands.cooldownchecker;

import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import constants.Settings;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CoolDownManager {

    private static final LoadingCache<Long, CoolDownUserData> coolDownUserDataMap = CacheBuilder.newBuilder()
            .expireAfterAccess(Settings.COOLDOWN_TIME_SEC, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public CoolDownUserData load(@NonNull Long userId) {
                    return new CoolDownUserData();
                }
            });

    public static synchronized CoolDownUserData getCoolDownData(long userId) {
        return coolDownUserDataMap.getUnchecked(userId);
    }

}