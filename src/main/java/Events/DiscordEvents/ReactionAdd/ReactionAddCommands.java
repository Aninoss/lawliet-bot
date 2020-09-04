package Events.DiscordEvents.ReactionAdd;

import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.ReactionAddAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

@DiscordEvent()
public class ReactionAddCommands extends ReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
