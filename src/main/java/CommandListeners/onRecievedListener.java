package CommandListeners;

import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public interface onRecievedListener {
    boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable;
}