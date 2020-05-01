package Core;

import Core.Utils.BotUtil;
import Modules.ImageCreator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ServerPatreonBoost {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerPatreonBoost.class);

    private static final ServerPatreonBoost ourInstance = new ServerPatreonBoost();

    private ServerPatreonBoost() {
    }

    public static ServerPatreonBoost getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Boolean> cache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<Long, Boolean>() {
                        @Override
                        public Boolean load(@NonNull Long serverId) {
                            Optional<Server> serverOptional = DiscordApiCollection.getInstance().getServerById(serverId);
                            if (serverOptional.isPresent()) {
                                Server server = serverOptional.get();

                                return server.getMembers().stream()
                                        .filter(user -> !user.isBot() && server.canManage(user))
                                        .anyMatch(user -> {
                                            try {
                                                return BotUtil.getUserDonationStatus(user) > 0;
                                            } catch (SQLException throwables) {
                                                LOGGER.error("Exception when checking donation status of user", throwables);
                                            }
                                            return false;
                                        });
                            }

                            return false;
                        }
                    }
            );

    public void setTrue(long serverId) {
        cache.put(serverId, true);
    }

    public void reset() {
        cache.invalidateAll();
    }

    public boolean get(long serverId) throws ExecutionException {
        return cache.get(serverId);
    }

}
