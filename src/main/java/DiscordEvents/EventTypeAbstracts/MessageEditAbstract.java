package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.MessageEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class MessageEditAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(MessageEditAbstract.class);

    public abstract boolean onMessageEdit(MessageEditEvent event) throws Throwable;

    public static void onMessageEditStatic(MessageEditEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof MessageEditAbstract) {
                MessageEditAbstract messageEditAbstract = (MessageEditAbstract) listener;

                try {
                    if (!messageEditAbstract.onMessageEdit(event)) return;
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
