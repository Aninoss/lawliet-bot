package events.discordevents.reactionadd;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ReactionAddAbstract;
import modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

@DiscordEvent()
public class ReactionAddCommands extends ReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
