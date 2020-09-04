package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.ArrayList;

public abstract class ServerLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerLeave(ServerLeaveEvent event) throws Throwable;

    public static void onServerLeaveStatic(ServerLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerLeaveAbstract) listener).onServerLeave(event)
        );
    }

}
