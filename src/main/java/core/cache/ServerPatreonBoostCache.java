package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class ServerPatreonBoostCache {

    private static final LoadingCache<Long, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Boolean load(@NonNull Long serverId) {
                            return ShardManager.getLocalGuildById(serverId).map(
                                    guild -> guild.getMembers().stream()
                                            .filter(member -> !member.getUser().isBot() && BotPermissionUtil.can(member, Permission.MANAGE_SERVER))
                                            .anyMatch(member -> PatreonCache.getInstance().hasPremium(member.getIdLong(), true))
                            ).orElse(false);
                        }
                    }
            );

    public static void setTrue(long serverId) {
        cache.put(serverId, true);
    }

    public static boolean get(long guildId) {
        try {
            return PatreonCache.getInstance().isUnlocked(guildId) || cache.get(guildId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Unknown error", e);
        }
        return false;
    }

}