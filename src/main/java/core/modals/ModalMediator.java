package core.modals;

import java.time.Duration;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.RandomUtil;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ModalMediator {

    private static final Cache<String, ModalConsumer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static Modal.Builder createModal(String title, ModalConsumer consumer) {
        String customId = RandomUtil.generateRandomString(20);
        cache.put(customId, consumer);
        return Modal.create(customId, title);
    }

    public static ModalConsumer get(String customId) {
        ModalConsumer consumer = cache.getIfPresent(customId);
        cache.invalidate(customId);
        return consumer;
    }

}
