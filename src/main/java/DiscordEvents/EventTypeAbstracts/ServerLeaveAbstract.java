package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerLeave(ServerLeaveEvent event) throws Throwable;

    public static void onServerLeaveStatic(ServerLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerLeaveAbstract) listener).onServerLeave(event)
        );
    }

}
