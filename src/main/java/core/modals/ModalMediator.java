package core.modals;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.listeners.Drawable;
import commands.runnables.NavigationAbstract;
import core.ExceptionLogger;
import core.utils.ExceptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.time.Duration;
import java.util.function.Function;

public class ModalMediator {

    private static final Cache<Long, ModalConsumer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static Modal.Builder createModal(long userId, String title, ModalConsumer consumer) {
        cache.put(userId, consumer);
        return Modal.create("modal", title);
    }

    public static Modal.Builder createDrawableCommandModal(Command command, String title, Function<ModalInteractionEvent, EmbedBuilder> consumer) {
        return createModal(command.getMemberId().get(), title, (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                EmbedBuilder eb = consumer.apply(e);

                if (command instanceof NavigationAbstract) {
                    NavigationAbstract navigationAbstractCommand = (NavigationAbstract) command;
                    navigationAbstractCommand.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                if (eb == null && command instanceof Drawable) {
                    Drawable drawableCommand = (Drawable) command;
                    eb = drawableCommand.draw(e.getMember());
                }
                command.drawMessage(eb).exceptionally(ExceptionLogger.get());
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });
    }

    public static ModalConsumer get(long userId) {
        ModalConsumer consumer = cache.getIfPresent(userId);
        cache.invalidate(userId);
        return consumer;
    }

}
