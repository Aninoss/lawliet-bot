package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        execute(listenerList, event.getUser(), false,
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
