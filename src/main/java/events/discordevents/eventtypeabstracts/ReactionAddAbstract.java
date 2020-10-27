package events.discordevents.eventtypeabstracts;

import core.DiscordApiCollection;
import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionAddAbstract.class);

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if ((event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory()) ||
                event.getUserId() == DiscordApiCollection.getInstance().getYourself().getId()
        ) {
            return;
        }

        if (event.getUser().isEmpty()) {
            event.getApi().getUserById(event.getUserId());
            return;
        }

        execute(listenerList, event.getUser().get(), false,
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
