package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.ArrayList;

public abstract class ServerLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerLeave(ServerLeaveEvent event) throws Throwable;

    public static void onServerLeaveStatic(ServerLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, true,
                listener -> ((ServerLeaveAbstract) listener).onServerLeave(event)
        );
    }

}
