package CommandListeners;

import Constants.Response;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

public interface onNavigationListener {
    Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable;
    boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable;
    EmbedBuilder draw(DiscordApi api, int state) throws Throwable;
    void onNavigationTimeOut(Message message) throws Throwable;
    int getMaxReactionNumber();
    default void onNewActivityOverwrite() {}
}