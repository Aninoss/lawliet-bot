package DiscordListener.ReactionAdd;

import CommandListeners.OnNavigationListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Core.ExceptionHandler;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import Modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation()
public class ReactionAddCommands extends ReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
