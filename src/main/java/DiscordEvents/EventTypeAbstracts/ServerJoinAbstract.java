package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerJoin(ServerJoinEvent event) throws Throwable;

    public static void onServerJoinStatic(ServerJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerJoinAbstract) listener).onServerJoin(event)
        );
    }

}
