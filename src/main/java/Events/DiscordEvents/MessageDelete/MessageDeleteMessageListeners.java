package Events.DiscordEvents.MessageDelete;

import CommandListeners.OnForwardedRecievedListener;
import CommandListeners.OnNavigationListener;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Events.DiscordEvents.DiscordEvent;
import Events.DiscordEvents.EventTypeAbstracts.MessageDeleteAbstract;
import org.javacord.api.event.message.MessageDeleteEvent;

import java.util.ArrayList;

@DiscordEvent()
public class MessageDeleteMessageListeners extends MessageDeleteAbstract {

    @Override
    public boolean onMessageDelete(MessageDeleteEvent event) throws Throwable {
        /* forwarded */
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

        /* reaction */
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

        return true;
    }

}
