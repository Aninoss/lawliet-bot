package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.message.MessageDeleteEvent;

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
