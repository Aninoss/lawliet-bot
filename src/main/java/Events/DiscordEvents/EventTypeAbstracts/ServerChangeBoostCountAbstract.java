package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerChangeBoostCountEvent;

import java.util.ArrayList;

public abstract class ServerChangeBoostCountAbstract extends DiscordEventAbstract {

    public abstract boolean onServerChangeBoostCount(ServerChangeBoostCountEvent event) throws Throwable;

    public static void onServerChangeBoostCountStatic(ServerChangeBoostCountEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerChangeBoostCountAbstract) listener).onServerChangeBoostCount(event)
        );
    }

}
