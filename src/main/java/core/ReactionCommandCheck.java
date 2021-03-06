package core;

import commands.CommandContainer;
import commands.listeners.OnReactionListener;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public class ReactionCommandCheck {

    public static boolean manage(GenericGuildMessageReactionEvent event) {
        CommandContainer.getInstance().getListeners(OnReactionListener.class).stream()
                .filter(listener -> listener.check(event))
                .forEach(listener -> ((OnReactionListener) listener.getCommand()).processReaction(event));

        return true;
    }

}
