package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ReactionRemoveAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ReactionRemoveAbstract.class);

    public abstract boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable;

    public static void onReactionRemoveStatic(ReactionRemoveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isBot() ||
                (!event.getMessage().isPresent() && !event.getChannel().canYouReadMessageHistory())
        ) return;

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ReactionRemoveAbstract) {
                ReactionRemoveAbstract reactionRemoveAbstract = (ReactionRemoveAbstract) listener;

                try {
                    if (!reactionRemoveAbstract.onReactionRemove(event)) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    LOGGER.error("Uncaught exception", throwable);
                }
            }
        }
    }

}
