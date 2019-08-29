package DiscordListener;

import CommandListeners.onForwardedRecievedListener;
import CommandListeners.onNavigationListener;
import CommandListeners.onReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import org.javacord.api.event.message.MessageDeleteEvent;

import java.util.ArrayList;

public class MessageDeleteListener {
    public MessageDeleteListener() {}

    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.getMessage().isPresent() || !event.getMessage().get().getUserAuthor().isPresent() || !event.getServer().isPresent() || event.getMessage().get().getUserAuthor().get().isBot()) return;

        //Entfernt Forwarded Listeners
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            long messageId = 0;
            if (command instanceof onForwardedRecievedListener) messageId = ((onForwardedRecievedListener) command).getForwardedMessage().getId();
            else if (command instanceof onNavigationListener) messageId = command.getNavigationMessage().getId();

            if (event.getMessageId() == messageId) {
                CommandContainer.getInstance().removeForwarder(command);
            }
        }

        //Entfernt Reaction Listeners
        list = CommandContainer.getInstance().getReactionInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            long messageId = 0;
            if (command instanceof onReactionAddListener) messageId = ((onReactionAddListener) command).getReactionMessage().getId();
            else if (command instanceof onNavigationListener) messageId = command.getNavigationMessage().getId();

            if (event.getMessageId() == messageId) {
                CommandContainer.getInstance().removeReactionListener(command);
            }
        }
    }
}