package modules.textai;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TextAICache {

    public static TextAICache ourInstance = new TextAICache();
    public static TextAICache getInstance() { return ourInstance; }
    private TextAICache() {}

    private final LoadingCache<String, TextAI.WordMap> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public TextAI.WordMap load(@NonNull String userOnServer) throws Exception {
                    return new TextAI.WordMap();
                }
            });

    public TextAI.WordMap get(long serverId, long userId, int contextSize) throws ExecutionException {
        return cache.get(generateKey(serverId, userId, contextSize));
    }

    public TextAI.WordMap get(long serverId, int contextSize) throws ExecutionException {
        return cache.get(generateKey(serverId, contextSize));
    }

    private String generateKey(long serverId, int contextSize) {
        StringBuilder sb = new StringBuilder()
                .append(serverId)
                .append(contextSize);

        return sb.toString();
    }

    private String generateKey(long serverId, long userId, int contextSize) {
        StringBuilder sb = new StringBuilder()
                .append(serverId)
                .append(userId)
                .append(contextSize);

        return sb.toString();
    }

}
