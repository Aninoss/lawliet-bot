package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.MessageEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class MessageEditAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageEdit(MessageEditEvent event) throws Throwable;

    public static void onMessageEditStatic(MessageEditEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        execute(event, listenerList,
                listener -> ((MessageEditAbstract) listener).onMessageEdit(event)
        );
    }

}
