package core;

import commands.CommandContainer;
import commands.CommandListenerMeta;
import commands.listeners.OnReactionListener;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

public class ReactionCommandCheck {

    public static boolean manage(GenericMessageReactionEvent event, EntityManagerWrapper entityManager) {
        CommandContainer.getListeners(OnReactionListener.class).stream()
                .filter(listener -> listener.check(event) == CommandListenerMeta.CheckResponse.ACCEPT)
                .forEach(listener -> ((OnReactionListener) listener.getCommand()).processReaction(event, entityManager));

        return true;
    }

}
