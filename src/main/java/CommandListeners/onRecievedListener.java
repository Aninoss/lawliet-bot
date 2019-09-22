package CommandListeners;

import org.javacord.api.event.message.MessageCreateEvent;

public interface onRecievedListener {
    boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable;
    default void execute() {
        CommandProperties commandProperties = (CommandProperties) this;
    }
}