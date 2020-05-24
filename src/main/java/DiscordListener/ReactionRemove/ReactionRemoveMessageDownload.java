package DiscordListener.ReactionRemove;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import DiscordListener.ListenerTypeAbstracts.ReactionRemoveAbstract;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class ReactionRemoveMessageDownload extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        try {
            if (event.getMessage().isPresent()) event.getMessage().get();
            else event.getChannel().getMessageById(event.getMessageId()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return false;
        }

        return true;
    }

}
