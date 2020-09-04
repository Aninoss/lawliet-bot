package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;

import java.util.ArrayList;

public abstract class ServerChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onServerChannelDelete(ServerChannelDeleteEvent event) throws Throwable;

    public static void onServerChannelDeleteStatic(ServerChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerChannelDeleteAbstract) listener).onServerChannelDelete(event)
        );
    }

}
