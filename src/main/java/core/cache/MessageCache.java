package core.cache;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;

public class MessageCache {

    private static final Cache<Long, Message> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(3))
            .build();

    public static synchronized CompletableFuture<Message> retrieveMessage(GuildMessageChannel channel, long messageId) {
        if (!BotPermissionUtil.canReadHistory(channel)) {
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

    public static synchronized void update(Message message) {
        if (cache.asMap().containsKey(message.getIdLong())) {
            cache.put(message.getIdLong(), message);
        }
    }

    public static synchronized void delete(long messageId) {
        cache.invalidate(messageId);
    }

}
