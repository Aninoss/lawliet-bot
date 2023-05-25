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

public class ExternalEmojiCache {

    private static final LoadingCache<Long, Optional<String>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                       @Override
                       public Optional<String> load(@NonNull Long emojiId) throws ExecutionException, InterruptedException {
                           return SendEvent.sendRequestCustomEmoji(emojiId).get();
                       }
                   }
            );

    public static Optional<String> getEmoteById(long emojiId) {
        try {
            return cache.get(emojiId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
            return Optional.empty();
        }
    }

}
