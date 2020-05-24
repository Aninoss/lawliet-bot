package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public abstract class MessageDeleteAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(MessageDeleteAbstract.class);

    public abstract boolean onMessageDelete(MessageDeleteEvent event) throws Throwable;

    public static void onMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof MessageDeleteAbstract) {
                MessageDeleteAbstract messageDeleteAbstract = (MessageDeleteAbstract) listener;

                try {
                    if (!messageDeleteAbstract.onMessageDelete(event)) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    LOGGER.error("Uncaught exception", throwable);
                }
            }
        }
    }

}
