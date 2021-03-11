package core.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import websockets.syncserver.SendEvent;

public class ExternalEmojiCache {

    private static final ExternalEmojiCache ourInstance = new ExternalEmojiCache();

    public static ExternalEmojiCache getInstance() {
        return ourInstance;
    }

    private ExternalEmojiCache() {
    }

    private final LoadingCache<Long, Optional<String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                       @Override
                       public Optional<String> load(@NonNull Long emojiId) throws ExecutionException, InterruptedException {
                           return SendEvent.sendRequestCustomEmoji(emojiId).get();
                       }
                   }
            );

    public Optional<String> getEmoteById(long emojiId) {
        try {
            return cache.get(emojiId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
            return Optional.empty();
        }
    }

}
