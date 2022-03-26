package core.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import events.sync.SendEvent;

public class ExternalGuildNameCache {

    private static final LoadingCache<Long, Optional<String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                       @Override
                       public Optional<String> load(@NonNull Long serverId) throws ExecutionException, InterruptedException {
                           return SendEvent.sendRequestServerName(serverId).get();
                       }
                   }
            );

    public static Optional<String> getGuildNameById(long serverId) {
        try {
            return cache.get(serverId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
            return Optional.empty();
        }
    }

}
