package modules;

import commands.Command;
import commands.CommandContainer;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnReactionAddListener;
import core.ExceptionHandler;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

public class ReactionCommandCheck {

    public static boolean manage(SingleReactionEvent event) {
        for (Command command : CommandContainer.getInstance().getReactionInstances()) {
            if (command != null && event.getMessageId() == command.getReactionMessageID()) {
                if (event.getUserId() == command.getReactionUserID()) {
                    try {
                        if (command instanceof OnReactionAddListener) command.onReactionAddSuper(event);
                        if (command instanceof OnNavigationListener) command.onNavigationReactionSuper(event);
                    } catch (Throwable e) {
                        ExceptionHandler.handleCommandException(e, command, event.getMessage().get().getChannel());
                    }
                } else {
                    if (event.getChannel().canYouRemoveReactionsOfOthers() && event.getReaction().isPresent())
                        event.getReaction().get().removeUser( event.getUser().get());
                }

                return false;
            }
        }

        return true;
    }

}
