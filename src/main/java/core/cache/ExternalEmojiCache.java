package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SendEvent;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ExternalEmojiCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalEmojiCache.class);

    private static final ExternalEmojiCache ourInstance = new ExternalEmojiCache();
    public static ExternalEmojiCache getInstance() { return ourInstance; }
    private ExternalEmojiCache() { }

    private final LoadingCache<Long, Optional<CustomEmoji>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                        @Override
                        public Optional<CustomEmoji> load(@NonNull Long emojiId) throws ExecutionException, InterruptedException {
                            return SendEvent.sendRequestCustomEmoji(emojiId).get();
                        }
                    }
            );

    public Optional<CustomEmoji> getCustomEmojiById(long emojiId) {
        try {
            return cache.get(emojiId);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
            return Optional.empty();
        }
    }

}
