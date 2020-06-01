package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerChangeBoostCountEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerChangeBoostCountAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerChangeBoostCountAbstract.class);

    public abstract boolean onServerChangeBoostCount(ServerChangeBoostCountEvent event) throws Throwable;

    public static void onServerChangeBoostCountStatic(ServerChangeBoostCountEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerChangeBoostCountAbstract) {
                ServerChangeBoostCountAbstract serverChangeBoostCountAbstract = (ServerChangeBoostCountAbstract) listener;

                try {
                    if (!serverChangeBoostCountAbstract.onServerChangeBoostCount(event)) return;
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
