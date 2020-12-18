package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import constants.AssetIds;
import constants.Settings;
import core.Bot;
import core.DiscordApiManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class PatreonCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PatreonCache.class);

    private static final PatreonCache ourInstance = new PatreonCache();
    private PatreonCache() {}
    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Integer> cache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Integer load(@NonNull Long userId) {
                            if (DiscordApiManager.getInstance().getOwnerId() == userId)
                                return Settings.PATREON_ROLE_IDS.length;
                            if (!Bot.isProductionMode()) return 0;

                            //TODO transfer to patreon api
                            Server supportServer = DiscordApiManager.getInstance().getLocalServerById(AssetIds.SUPPORT_SERVER_ID).get();
                            AtomicInteger status = new AtomicInteger(0);

                            supportServer.getMemberById(userId).ifPresent(user -> {
                                for (int i = Settings.PATREON_ROLE_IDS.length - 1; i >= 0; i--) {
                                    Role role = supportServer.getRoleById(Settings.PATREON_ROLE_IDS[i]).get();
                                    if (user.getRoles(supportServer).contains(role)) {
                                        status.set(i + 1);
                                        break;
                                    }
                                }
                            });

                            return status.get();
                        }
                    }
            );

    public void reset() {
        cache.invalidateAll();
    }

    public void resetUser(long userId) {
        cache.refresh(userId);
    }

    public int getPatreonLevel(long userId) {
        try {
            return cache.get(userId);
        } catch (ExecutionException e) {
            LOGGER.error("Exception in Patreon check", e);
        }
        return 0;
    }

    public void setPatreonLevel(long userId, int level) {
        cache.put(userId, level);
    }

}
