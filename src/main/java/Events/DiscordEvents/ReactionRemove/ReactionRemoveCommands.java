package Events.DiscordEvents.ReactionRemove;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.ReactionRemoveAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordEvent()
public class ReactionRemoveCommands extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
