package core.cache;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.Settings;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class MessageCache {

    private static final MessageCache ourInstance = new MessageCache();

    public static MessageCache getInstance() {
        return ourInstance;
    }

    private MessageCache() {
    }

    private final Cache<Long, Message> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private final Cache<Long, Boolean> cacheMessageBlock = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public synchronized CompletableFuture<Message> retrieveMessage(TextChannel channel, long messageId) {
        Boolean block = cacheMessageBlock.getIfPresent(messageId);
        if ((block != null && block) || !BotPermissionUtil.canReadHistory(channel)) {
            return CompletableFuture.failedFuture(new NoSuchElementException("No such message"));
        }

        if (cache.asMap().containsKey(messageId)) {
            return CompletableFuture.completedFuture(cache.getIfPresent(messageId));
        } else {
            return channel.retrieveMessageById(messageId).submit()
                    .thenApply(message -> {
                        cache.put(messageId, message);
                        return message;
                    });
        }
    }

    public synchronized void put(Message message) {
        if (Settings.CACHE_NEW_MESSAGES || cache.asMap().containsKey(message.getIdLong())) {
            cache.put(message.getIdLong(), message);
        }
    }

    public synchronized void block(long messageId) {
        cacheMessageBlock.put(messageId, true);
    }

    public synchronized void delete(long messageId) {
        cache.invalidate(messageId);
        cacheMessageBlock.invalidate(messageId);
    }

}
