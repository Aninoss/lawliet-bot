package modules.porn;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.List;

public class BooruTagCache {

    private static final Cache<String, List<String>> tagCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static void addTags(String mediaUrl, List<String> tags) {
        tagCache.put(mediaUrl, tags);
    }

    public static List<String> getTags(String mediaUrl) {
        return tagCache.getIfPresent(mediaUrl);
    }

}
