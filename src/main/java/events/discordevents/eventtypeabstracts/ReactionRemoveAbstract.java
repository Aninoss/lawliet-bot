package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.ArrayList;

public abstract class ReactionRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable;

    public static void onReactionRemoveStatic(ReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        execute(listenerList, event.getUser().get(), false,
                listener -> ((ReactionRemoveAbstract) listener).onReactionRemove(event)
        );
    }

}
