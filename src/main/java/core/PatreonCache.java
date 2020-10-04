package core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import constants.AssetIds;
import constants.Settings;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class PatreonCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PatreonCache.class);

    private static final PatreonCache ourInstance = new PatreonCache();
    private PatreonCache() {}
    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Integer> cache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<Long, Integer>() {
                        @Override
                        public Integer load(@NonNull Long userId) throws SQLException {
                            if (DiscordApiCollection.getInstance().getOwnerId() == userId)
                                return Settings.PATREON_ROLE_IDS.length;
                            if (!Bot.isProductionMode()) return 0;

                            Server supportServer = DiscordApiCollection.getInstance().getServerById(AssetIds.SUPPORT_SERVER_ID).get();
                            for (int i = 0; i < Settings.PATREON_ROLE_IDS.length; i++) {
                                if (supportServer.getRoleById(Settings.PATREON_ROLE_IDS[i]).get().getUsers().stream().anyMatch(user -> user.getId() == userId))
                                    return i + 1;
                            }

                            return 0;
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

}
