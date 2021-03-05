package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SendEvent;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ExternalServerNameCache {
    
    private static final ExternalServerNameCache ourInstance = new ExternalServerNameCache();
    public static ExternalServerNameCache getInstance() { return ourInstance; }
    private ExternalServerNameCache() { }

    private final LoadingCache<Long, Optional<String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                        @Override
                        public Optional<String> load(@NonNull Long serverId) throws ExecutionException, InterruptedException {
                            return SendEvent.sendRequestServerName(serverId).get();
                        }
                    }
            );

    public Optional<String> getServerNameById(long serverId) {
        try {
            return cache.get(serverId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
            return Optional.empty();
        }
    }

}
