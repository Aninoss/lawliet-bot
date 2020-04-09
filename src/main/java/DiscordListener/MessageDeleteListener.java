package DiscordListener;

import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnNavigationListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import org.javacord.api.event.message.MessageDeleteEvent;

import java.util.ArrayList;

public class MessageDeleteListener {

    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.getMessage().isPresent() || !event.getMessage().get().getUserAuthor().isPresent() || !event.getServer().isPresent() || event.getMessage().get().getUserAuthor().get().isBot()) return;

        //Entfernt Forwarded Listeners
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            long messageId = 0;
            if (command != null) {
                if (command instanceof OnForwardedRecievedListener)
                    messageId = ((OnForwardedRecievedListener) command).getForwardedMessage().getId();
                else if (command instanceof OnNavigationListener) messageId = command.getNavigationMessage().getId();

                if (event.getMessageId() == messageId) {
                    CommandContainer.getInstance().removeForwarder(command);
                }
            }
        }

        //Entfernt Reaction Listeners
        list = CommandContainer.getInstance().getReactionInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            long messageId = 0;
            if (command != null) {
                if (command instanceof OnReactionAddListener)
                    messageId = ((OnReactionAddListener) command).getReactionMessage().getId();
                else if (command instanceof OnNavigationListener) messageId = command.getNavigationMessage().getId();

                if (event.getMessageId() == messageId) {
                    CommandContainer.getInstance().removeReactionListener(command);
                }
            }
        }
    }
}