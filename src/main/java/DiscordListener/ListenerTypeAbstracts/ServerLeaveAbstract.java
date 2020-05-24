package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerLeaveAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerLeaveAbstract.class);

    public abstract boolean onServerLeave(ServerLeaveEvent event) throws Throwable;

    public static void onServerLeaveStatic(ServerLeaveEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        for(DiscordListenerAbstract listener : listenerList) {
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
