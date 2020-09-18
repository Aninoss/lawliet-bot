package events.discordevents.reactionremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ReactionRemoveAbstract;
import modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordEvent()
public class ReactionRemoveCommands extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
