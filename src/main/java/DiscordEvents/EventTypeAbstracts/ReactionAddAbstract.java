package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ReactionAddAbstract.class);

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isBot() ||
                (!event.getMessage().isPresent() && !event.getChannel().canYouReadMessageHistory())
        ) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ReactionAddAbstract) {
                ReactionAddAbstract reactionAddAbstract = (ReactionAddAbstract) listener;
                if (banned && !reactionAddAbstract.isAllowingBannedUser()) continue;

                try {
                    if (!reactionAddAbstract.onReactionAdd(event)) return;
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
