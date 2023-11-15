package mysql.redis.fisheryusers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import mysql.redis.RedisManager;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Pipeline;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FisheryUserManager {

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
            List<String> accountKeys = RedisManager.scan(jedis, "fishery_account:" + guildId + ":*");
            Pipeline pipeline = jedis.pipelined();
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

    public static List<Long> getGuildIdsForFisheryUser(long userId) {
        ArrayList<Long> guildIds = new ArrayList<>();
        RedisManager.update(jedis -> {
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
        });
        return guildIds;
    }

}
