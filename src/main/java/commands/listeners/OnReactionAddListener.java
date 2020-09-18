package commands.listeners;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

public interface OnReactionAddListener {

    void onReactionAdd(SingleReactionEvent event) throws Throwable;
    Message getReactionMessage();
    void onReactionTimeOut(Message message) throws Throwable;

}