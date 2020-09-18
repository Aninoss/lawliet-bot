package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.message.MessageDeleteEvent;

import java.util.ArrayList;

public abstract class MessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageDelete(MessageDeleteEvent event) throws Throwable;

    public static void onMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getServer().isPresent())
            return;

        execute(event, listenerList,
                listener -> ((MessageDeleteAbstract) listener).onMessageDelete(event)
        );
    }

}
