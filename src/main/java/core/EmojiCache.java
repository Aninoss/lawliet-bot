package core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.server.Server;
import java.util.concurrent.ExecutionException;

public class EmojiCache {

    private static final EmojiCache ourInstance = new EmojiCache();
    public static EmojiCache getInstance() { return ourInstance; }
    private EmojiCache() {}

    private final LoadingCache<Long, KnownCustomEmoji> emojiCache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<>() {
                        @Override
                        public KnownCustomEmoji load(@NonNull Long emojiId) {
                            Server server = DiscordApiCollection.getInstance().getHomeServer();
                            return server.getCustomEmojiById(emojiId).get();
                        }
                    }
            );

    private final LoadingCache<String, Long> emojiIdCache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Long load(@NonNull String emojiName) {
                            Server server = DiscordApiCollection.getInstance().getHomeServer();
                            KnownCustomEmoji[] knownCustomEmojis = new KnownCustomEmoji[0];
                            KnownCustomEmoji emoji = server.getCustomEmojisByName(emojiName).toArray(knownCustomEmojis)[0];
                            emojiCache.put(emoji.getId(), emoji);

                            return emoji.getId();
                        }
                    }
            );

    public KnownCustomEmoji getHomeEmojiById(long emojiId) throws ExecutionException {
        return emojiCache.get(emojiId);
    }

    public KnownCustomEmoji getHomeEmojiByName(String emojiName) throws ExecutionException {
        return emojiCache.get(emojiIdCache.get(emojiName));
    }

}
