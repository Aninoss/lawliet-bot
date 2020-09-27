package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageDeleteEvent;

import java.util.ArrayList;

public abstract class MessageDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageDelete(MessageDeleteEvent event) throws Throwable;

    public static void onMessageDeleteStatic(MessageDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        User user = event.getMessageAuthor().flatMap(MessageAuthor::asUser).orElse(null);
        if (event.getServer().isEmpty() || user == null)
            return;

        execute(listenerList, user, false,
                listener -> ((MessageDeleteAbstract) listener).onMessageDelete(event)
        );
    }

}
