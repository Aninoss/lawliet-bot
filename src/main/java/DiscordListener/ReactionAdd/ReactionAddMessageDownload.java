package DiscordListener.ReactionAdd;

import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnNavigationListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.MessageDeleteAbstract;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class ReactionAddMessageDownload extends ReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
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
