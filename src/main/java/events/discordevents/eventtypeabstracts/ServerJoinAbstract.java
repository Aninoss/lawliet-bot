package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerJoinEvent;

import java.util.ArrayList;

public abstract class ServerJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerJoin(ServerJoinEvent event) throws Throwable;

    public static void onServerJoinStatic(ServerJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, true, event.getServer().getId(),
                listener -> ((ServerJoinAbstract) listener).onServerJoin(event)
        );
    }

}
