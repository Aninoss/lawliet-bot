package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerChannelDeleteAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerChannelDeleteAbstract.class);

    public abstract boolean onServerChannelDelete(ServerChannelDeleteEvent event) throws Throwable;

    public static void onServerChannelDeleteStatic(ServerChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerChannelDeleteAbstract) {
                ServerChannelDeleteAbstract serverChannelDeleteAbstract = (ServerChannelDeleteAbstract) listener;

                try {
                    if (!serverChannelDeleteAbstract.onServerChannelDelete(event)) return;
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
