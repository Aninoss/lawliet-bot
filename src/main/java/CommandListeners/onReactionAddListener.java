package CommandListeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public interface onReactionAddListener {
    void onReactionAdd(SingleReactionEvent event) throws Throwable;
    Message getReactionMessage();
    void onReactionTimeOut(Message message) throws Throwable;
}