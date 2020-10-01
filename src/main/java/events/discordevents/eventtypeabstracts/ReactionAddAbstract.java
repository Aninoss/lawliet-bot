package events.discordevents.eventtypeabstracts;

import core.DiscordApiCollection;
import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        User user;
        if (event.getServer().isPresent() && event.getUser().isEmpty())
            user = DiscordApiCollection.getInstance().fetchUserById(event.getServer().get(), event.getUserId()).get();
        else
            user = event.getUser().get();

        execute(listenerList, user, false,
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
