package DiscordEvents.ReactionAdd;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ReactionAddAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

@DiscordEventAnnotation()
public class ReactionAddCommands extends ReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
