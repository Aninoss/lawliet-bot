package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isBot() ||
                (!event.getMessage().isPresent() && !event.getChannel().canYouReadMessageHistory())
        ) return;

        execute(event, listenerList,
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

    public static void lol(ArrayList<String> lol) {

    }

}
