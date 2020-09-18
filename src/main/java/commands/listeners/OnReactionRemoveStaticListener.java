package commands.listeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

public interface OnReactionRemoveStaticListener {

    void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws Throwable;
    String getTitleStartIndicator();

}