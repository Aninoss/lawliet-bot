package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionAddAbstract.class);

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory())
            return;

        //TODO debug
        User user = event.getUser();
        /*User user;
        if (event.getServer().isPresent() && event.getUser().isEmpty()) {
            user = DiscordApiCollection.getInstance().fetchUserById(event.getServer().get(), event.getUser().getId()).get();
            LOGGER.info("### USER FETCHED {} ###", user.getId());
        } else {
            user =  event.getUser();
        }*/

        execute(listenerList, user, false,
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
