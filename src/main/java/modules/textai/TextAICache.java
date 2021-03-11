package modules.textai;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TextAICache {

    public static TextAICache ourInstance = new TextAICache();

    public static TextAICache getInstance() {
        return ourInstance;
    }

    private TextAICache() {
    }

    private final LoadingCache<String, TextAI.WordMap> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public TextAI.WordMap load(@NonNull String userOnServer) {
                    return new TextAI.WordMap();
                }
            });

    public TextAI.WordMap get(long serverId, long userId, int contextSize) {
        try {
            return cache.get(generateKey(serverId, userId, contextSize));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public TextAI.WordMap get(long serverId, int contextSize) {
        try {
            return cache.get(generateKey(serverId, contextSize));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateKey(long serverId, int contextSize) {
        return String.valueOf(serverId) + contextSize;
    }

    private String generateKey(long serverId, long userId, int contextSize) {
        return String.valueOf(serverId) + userId + contextSize;
    }

}
