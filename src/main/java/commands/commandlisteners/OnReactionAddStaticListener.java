package commands.commandlisteners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

public interface OnReactionAddStaticListener {

    void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable;
    String getTitleStartIndicator();

}