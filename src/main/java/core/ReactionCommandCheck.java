package core;

import commands.CommandContainer;
import commands.CommandListenerMeta;
import commands.listeners.OnReactionListener;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

public class ReactionCommandCheck {

    public static boolean manage(GenericMessageReactionEvent event) {
        if (event.getChannel() instanceof TextChannel && BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
            CommandContainer.getListeners(OnReactionListener.class).stream()
                    .filter(listener -> listener.check(event) == CommandListenerMeta.CheckResponse.ACCEPT)
                    .forEach(listener -> ((OnReactionListener) listener.getCommand()).processReaction(event));
        }

        return true;
    }

}
