package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import DiscordEvents.EventPriority;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public abstract class MessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageDelete(MessageDeleteEvent event) throws Throwable;

    public static void onMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        execute(event, listenerList,
                listener -> ((MessageDeleteAbstract) listener).onMessageDelete(event)
        );
    }

}
