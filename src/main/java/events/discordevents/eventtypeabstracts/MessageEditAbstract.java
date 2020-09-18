package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.message.MessageEditEvent;

import java.util.ArrayList;

public abstract class MessageEditAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageEdit(MessageEditEvent event) throws Throwable;

    public static void onMessageEditStatic(MessageEditEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getServer().isPresent())
            return;

        execute(event, listenerList,
                listener -> ((MessageEditAbstract) listener).onMessageEdit(event)
        );
    }

}
