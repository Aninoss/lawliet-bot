package core.cache;

import java.time.Duration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TicketProtocolCache {

    private static final Cache<Long, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public static String getUrl(long channelId) {
        return cache.getIfPresent(channelId);
    }

    public static void setUrl(long channelId, String url) {
        cache.put(channelId, url);
    }

}
