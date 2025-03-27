package mysql.redis.fisheryusers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import mysql.redis.RedisManager;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FisheryUserManager {

    public static final String KEY_FISHERY_GUILDS_BY_USER = "fishery_guilds_by_user:";
    public static final String KEY_FISHERY_USERS_BY_GUILD = "fishery_users_by_guild:";

    private static final LoadingCache<Long, FisheryGuildData> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build(new CacheLoader<>() {
                @Override
                public FisheryGuildData load(@NotNull Long guildId) {
                    return new FisheryGuildData(guildId);
                }
            });

    public static FisheryGuildData getGuildData(long guildId) {
        try {
            return cache.get(guildId);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteGuildData(long guildId) {
        FisheryGuildData fisheryGuildData = getGuildData(guildId);
        RedisManager.update(jedis -> {
            Set<String> userIds = jedis.hkeys(KEY_FISHERY_USERS_BY_GUILD + guildId);
            List<String> accountKeys = RedisManager.scan(jedis, "fishery_account:" + guildId + ":*");

            Pipeline pipeline = jedis.pipelined();
            for (String userId : userIds) {
                pipeline.hdel(KEY_FISHERY_GUILDS_BY_USER + userId, String.valueOf(guildId));
            }
            pipeline.del(KEY_FISHERY_USERS_BY_GUILD + guildId);
            pipeline.del(fisheryGuildData.KEY_RECENT_FISH_GAINS_RAW);
            pipeline.del(fisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED);
            pipeline.del(accountKeys.toArray(new String[0]));
            pipeline.sync();
        });
        cache.invalidate(guildId);
    }

    public static void copyGuildData(long oldGuildId, long newGuildId) {
        FisheryGuildData oldFisheryGuildData = getGuildData(oldGuildId);
        FisheryGuildData newFisheryGuildData = getGuildData(newGuildId);
        RedisManager.update(jedis -> {
            List<Map.Entry<String, String>> oldRecentFishGainsRawEntries = RedisManager.hscan(jedis, oldFisheryGuildData.KEY_RECENT_FISH_GAINS_RAW);
            List<String> oldAccountKeys = RedisManager.scan(jedis, "fishery_account:" + oldGuildId + ":*");

            Pipeline pipeline = jedis.pipelined();
            pipeline.copy(oldFisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED, newFisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED, true);
            oldRecentFishGainsRawEntries.forEach(entry -> pipeline.hset(newFisheryGuildData.KEY_RECENT_FISH_GAINS_RAW, entry.getKey(), entry.getValue()));
            oldAccountKeys.forEach(key -> pipeline.copy(key, key.replace(String.valueOf(oldGuildId), String.valueOf(newGuildId)), true));
            pipeline.sync();
        });
    }

    public static void setUserActiveOnGuild(Pipeline pipeline, FisheryMemberData fisheryMemberData) {
        setUserActiveOnGuild(pipeline, fisheryMemberData.getGuildId(), fisheryMemberData.getMemberId());
    }

    public static void setUserActiveOnGuild(Pipeline pipeline, long guildId, long userId) {
        pipeline.hset(KEY_FISHERY_GUILDS_BY_USER + userId, String.valueOf(guildId), Instant.now().toString());
        pipeline.hset(KEY_FISHERY_USERS_BY_GUILD + guildId, String.valueOf(userId), Instant.now().toString());
    }

    public static void deleteUserActiveOnGuild(Pipeline pipeline, FisheryMemberData fisheryMemberData) {
        deleteUserActiveOnGuild(pipeline, fisheryMemberData.getGuildId(), fisheryMemberData.getMemberId());
    }

    public static void deleteUserActiveOnGuild(Pipeline pipeline, long guildId, long userId) {
        pipeline.hdel(FisheryUserManager.KEY_FISHERY_GUILDS_BY_USER + userId, String.valueOf(guildId));
        pipeline.hdel(FisheryUserManager.KEY_FISHERY_USERS_BY_GUILD + guildId, String.valueOf(userId));
    }

    public static Set<Long> getGuildIdsByUserId(long userId, boolean slowSearch) {
        return RedisManager.get(jedis -> {
            HashSet<Long> guildIds = new HashSet<>();
            jedis.hkeys(KEY_FISHERY_GUILDS_BY_USER + userId).stream()
                    .map(Long::parseLong)
                    .forEach(guildIds::add);

            if (slowSearch) {
                List<String> accountKeys = RedisManager.scan(jedis, "fishery_account:*:" + userId);
                for (String accountKey : accountKeys) {
                    String[] parts = accountKey.split(":");
                    long fisheryGuildId = Long.parseLong(parts[1]);
                    long fisheryUserId = Long.parseLong(parts[2]);
                    if (fisheryUserId == userId) {
                        guildIds.add(fisheryGuildId);
                    } else {
                        MainLogger.get().error("Returning wrong entries for fishery user");
                    }
                }
            }

            return guildIds;
        });
    }

    public static Set<Long> getUserIdsByGuildId(long guildId) {
        return RedisManager.get(jedis -> {
            HashSet<Long> userIds = new HashSet<>();
            jedis.hkeys(KEY_FISHERY_USERS_BY_GUILD + guildId).stream()
                    .map(Long::parseLong)
                    .forEach(userIds::add);
            return userIds;
        });
    }

}
