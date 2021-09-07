package events.discordevents;

import java.util.function.Consumer;
import commands.CommandContainer;
import commands.CommandListenerMeta;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent;

public interface InteractionListenerHandler <T extends GenericComponentInteractionCreateEvent> {

    default void handleInteraction(T event, Class<?> clazz, Consumer<CommandListenerMeta<?>> listenerMetaConsumer) {
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
            CommandContainer.getListeners(clazz)
                    .forEach(listener -> {
                        switch (listener.check(event)) {
                            case ACCEPT -> listenerMetaConsumer.accept(listener);
                            case DENY -> {
                                EmbedBuilder eb = EmbedFactory.getEmbedError(
                                        listener.getCommand(),
                                        TextManager.getString(listener.getCommand().getLocale(), TextManager.GENERAL, "button_listener_denied", listener.getCommand().getMemberAsMention().get())
                                );
                                event.replyEmbeds(eb.build())
                                        .setEphemeral(true)
                                        .queue();
                            }
                        }
                    });
        }
    }

}
