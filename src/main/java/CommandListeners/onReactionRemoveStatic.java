package CommandListeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

public interface onReactionRemoveStatic {
    void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws Throwable;
    boolean requiresLocale();
    String getTitleStartIndicator();
}