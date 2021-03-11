package events.discordevents.eventtypeabstracts;

import java.util.ArrayList;
import events.discordevents.DiscordEventAbstract;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public abstract class PrivateMessageReceivedAbstract extends DiscordEventAbstract {

    public abstract boolean onPrivateMessageReceived(PrivateMessageReceivedEvent event) throws Throwable;

    public static void onPrivateMessageReceivedStatic(PrivateMessageReceivedEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getAuthor(),
                listener -> ((PrivateMessageReceivedAbstract) listener).onPrivateMessageReceived(event)
        );
    }

}
