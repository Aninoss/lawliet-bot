package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageEditEvent;

import java.util.ArrayList;

public abstract class MessageEditAbstract extends DiscordEventAbstract {

    public abstract boolean onMessageEdit(MessageEditEvent event) throws Throwable;

    public static void onMessageEditStatic(MessageEditEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        User user = event.getMessageAuthor().flatMap(MessageAuthor::asUser).orElse(null);
        if (event.getServer().isEmpty() || user == null)
            return;

        execute(listenerList, user, false, event.getServer().map(DiscordEntity::getId).orElse(0L),
                listener -> ((MessageEditAbstract) listener).onMessageEdit(event)
        );
    }

}
