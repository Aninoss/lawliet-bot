package Modules;

import CommandListeners.OnNavigationListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Core.ExceptionHandler;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactionCommandCheck {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionCommandCheck.class);

    public static boolean manage(SingleReactionEvent event) {
        for (Command command : CommandContainer.getInstance().getReactionInstances()) {
            if (command != null && event.getMessageId() == command.getReactionMessageID()) {
                if (event.getUser().getId() == command.getReactionUserID()) {
                    try {
                        if (command instanceof OnReactionAddListener) command.onReactionAddSuper(event);
                        if (command instanceof OnNavigationListener) command.onNavigationReactionSuper(event);
                    } catch (Throwable e) {
                        ExceptionHandler.handleCommandException(e, command, event.getMessage().get().getChannel());
                    }
                } else {
                    if (event.getChannel().canYouRemoveReactionsOfOthers() && event.getReaction().isPresent())
                        event.getReaction().get().removeUser(event.getUser());
                }

                return false;
            }
        }

        return true;
    }

}
