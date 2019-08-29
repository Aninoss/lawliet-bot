package CommandListeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

public interface onReactionAddStatic {
    void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable;
    boolean requiresLocale();
    String getTitleStartIndicator();
}