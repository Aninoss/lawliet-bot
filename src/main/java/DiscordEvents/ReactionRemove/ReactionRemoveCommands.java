package DiscordEvents.ReactionRemove;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ReactionRemoveAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordEventAnnotation()
public class ReactionRemoveCommands extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
