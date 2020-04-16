package CommandListeners;

import Constants.Response;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

public interface OnForwardedRecievedListener {

    Response onForwardedRecieved(MessageCreateEvent event) throws Throwable;
    Message getForwardedMessage();
    void onForwardedTimeOut() throws Throwable;
    default void onNewActivityOverwrite() {}

}