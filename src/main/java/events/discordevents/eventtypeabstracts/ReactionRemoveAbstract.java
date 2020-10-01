package events.discordevents.eventtypeabstracts;

import core.DiscordApiCollection;
import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.ArrayList;

public abstract class ReactionRemoveAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable;

    public static void onReactionRemoveStatic(ReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        User user;
        if (event.getServer().isPresent() && event.getUser().isEmpty())
            user = DiscordApiCollection.getInstance().fetchUserById(event.getServer().get(), event.getUserId()).get();
        else
            user = event.getUser().get();

        execute(listenerList, user, false,
                listener -> ((ReactionRemoveAbstract) listener).onReactionRemove(event)
        );
    }

}
