package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerJoinAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinAbstract.class);

    public abstract boolean onServerJoin(ServerJoinEvent event) throws Throwable;

    public static void onServerJoinStatic(ServerJoinEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof ServerJoinAbstract) {
                ServerJoinAbstract serverJoinAbstract = (ServerJoinAbstract) listener;

                try {
                    if (!serverJoinAbstract.onServerJoin(event)) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    LOGGER.error("Uncaught exception", throwable);
                }
            }
        }
    }

}
