package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerLeaveAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerLeaveAbstract.class);

    public abstract boolean onServerLeave(ServerLeaveEvent event) throws Throwable;

    public static void onServerLeaveStatic(ServerLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerLeaveAbstract) {
                ServerLeaveAbstract serverLeaveAbstract = (ServerLeaveAbstract) listener;

                try {
                    if (!serverLeaveAbstract.onServerLeave(event)) return;
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
