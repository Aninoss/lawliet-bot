package core;

import commands.CommandContainer;
import commands.CommandListenerMeta;
import commands.listeners.OnReactionListener;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

public class ReactionCommandCheck {

    public static boolean manage(GenericMessageReactionEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
            CommandContainer.getListeners(OnReactionListener.class).stream()
                    .filter(listener -> listener.check(event) == CommandListenerMeta.CheckResponse.ACCEPT)
                    .forEach(listener -> ((OnReactionListener) listener.getCommand()).processReaction(event));
        }

        return true;
    }

}
