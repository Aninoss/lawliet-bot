package CommandListeners;

import Constants.ActionType;
import Constants.Response;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public interface onNavigationListener {
    Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable;
    boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable;
    EmbedBuilder draw(DiscordApi api) throws Throwable;
    void onNavigationTimeOut(Message message) throws Throwable;
    int getMaxReactionNumber();
}