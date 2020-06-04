package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onServerChannelDelete(ServerChannelDeleteEvent event) throws Throwable;

    public static void onServerChannelDeleteStatic(ServerChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerChannelDeleteAbstract) listener).onServerChannelDelete(event)
        );
    }

}
