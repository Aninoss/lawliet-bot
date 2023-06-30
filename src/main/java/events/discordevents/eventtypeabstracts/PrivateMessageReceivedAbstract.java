package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class PrivateMessageReceivedAbstract extends DiscordEventAbstract {

    public abstract boolean onPrivateMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable;

    public static void onPrivateMessageReceivedStatic(MessageReceivedEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getAuthor(),
                (listener, entityManager) -> ((PrivateMessageReceivedAbstract) listener).onPrivateMessageReceived(event, entityManager)
        );
    }

}
