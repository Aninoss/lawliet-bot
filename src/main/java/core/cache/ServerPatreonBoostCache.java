package core.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServerPatreonBoostCache {

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
                            Optional<Guild> guildOptional = ShardManager.getInstance().getLocalGuildById(serverId);
                            if (guildOptional.isPresent()) {
                                Guild guild = guildOptional.get();

                                return guild.getMembers().stream()
                                        .filter(member -> !member.getUser().isBot() && BotPermissionUtil.can(member, Permission.MANAGE_SERVER))
                                        .anyMatch(member -> PatreonCache.getInstance().getUserTier(member.getIdLong()) > 1);
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
        return false;
    }

}