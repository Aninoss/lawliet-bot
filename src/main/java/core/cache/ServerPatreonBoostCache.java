package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.DiscordApiManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ServerPatreonBoostCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerPatreonBoostCache.class);

    private static final ServerPatreonBoostCache ourInstance = new ServerPatreonBoostCache();

    private ServerPatreonBoostCache() {
    }

    public static ServerPatreonBoostCache getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Boolean load(@NonNull Long serverId) {
                            Optional<Server> serverOptional = DiscordApiManager.getInstance().getLocalGuildById(serverId);
                            if (serverOptional.isPresent()) {
                                Server server = serverOptional.get();

                                return server.getMembers().stream()
                                        .filter(user -> !user.isBot() && server.canManage(user))
                                        .anyMatch(user -> PatreonCache.getInstance().getUserTier(user.getId()) > 1);
                            }

                            return false;
                        }
                    }
            );

    public void setTrue(long serverId) {
        cache.put(serverId, true);
    }

    public boolean get(long serverId) {
        try {
            return cache.get(serverId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Unknown error", e);
        }
    }

}