package CommandListeners;

import Constants.Response;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public interface OnForwardedRecievedListener {

    Response onForwardedRecieved(MessageCreateEvent event) throws Throwable;
    Message getForwardedMessage();
    void onForwardedTimeOut() throws Throwable;
    default void onNewActivityOverwrite() {}

}