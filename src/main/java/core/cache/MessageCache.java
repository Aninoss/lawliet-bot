package core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import java.time.Duration;

public class MessageCache {

    private static final MessageCache ourInstance = new MessageCache();

    public static MessageCache getInstance() {
        return ourInstance;
    }

    private MessageCache() {
    }

    private final Cache<Long, Message> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public synchronized Message get(TextChannel channel, long messageId) {
        if (cache.asMap().containsKey(messageId)) {
            return cache.getIfPresent(messageId);
        } else {
            Message message = channel.retrieveMessageById(messageId).complete();
            cache.put(messageId, message);
            return message;
        }
    }

    public synchronized void update(Message message) {
        if (cache.asMap().containsKey(message.getIdLong())) {
            cache.put(message.getIdLong(), message);
        }
    }

    public synchronized void delete(long messageId) {
        cache.invalidate(messageId);
    }

}
