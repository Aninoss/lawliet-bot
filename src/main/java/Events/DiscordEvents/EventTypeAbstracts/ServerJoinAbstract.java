package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerJoinEvent;

import java.util.ArrayList;

public abstract class ServerJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerJoin(ServerJoinEvent event) throws Throwable;

    public static void onServerJoinStatic(ServerJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerJoinAbstract) listener).onServerJoin(event)
        );
    }

}
