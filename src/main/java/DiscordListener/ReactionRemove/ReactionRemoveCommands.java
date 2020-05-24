package DiscordListener.ReactionRemove;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import DiscordListener.ListenerTypeAbstracts.ReactionRemoveAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordListenerAnnotation()
public class ReactionRemoveCommands extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
