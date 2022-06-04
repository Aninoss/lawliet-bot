package core;

import java.time.Duration;
import java.util.function.Consumer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.RandomUtil;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.Modal;

public class ModalMediator {

    private static final Cache<String, Consumer<ModalInteractionEvent>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static Modal.Builder createModal(String title, Consumer<ModalInteractionEvent> consumer) {
        String customId = RandomUtil.generateRandomString(20);
        cache.put(customId, consumer);
        return Modal.create(customId, title);
    }

    public static Consumer<ModalInteractionEvent> get(String customId) {
        Consumer<ModalInteractionEvent> consumer = cache.getIfPresent(customId);
        cache.invalidate(customId);
        return consumer;
    }

}
