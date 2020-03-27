package CommandListeners;

import org.javacord.api.event.message.MessageCreateEvent;

public interface onRecievedListener {
    boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable;
}