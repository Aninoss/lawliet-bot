package modules.casinologs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CasinoLogCache {

    private static final LoadingCache<Integer, ArrayList<CasinoLogEntry>> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofDays(2))
            .build(new CacheLoader<>() {
                @Override
                public ArrayList<CasinoLogEntry> load(@NotNull Integer hash) {
                    return new ArrayList<>();
                }
            });

    public static ArrayList<CasinoLogEntry> get(long serverId, long userId) {
        try {
            return cache.get(Objects.hash(serverId, userId));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void put(long serverId, long userId, CasinoLogEntry entry) {
        get(serverId, userId).add(entry);
    }

}
