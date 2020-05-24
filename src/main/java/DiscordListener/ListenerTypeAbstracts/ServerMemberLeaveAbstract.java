package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerMemberLeaveAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeaveAbstract.class);

    public abstract boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable;

    public static void onServerMemberLeaveStatic(ServerMemberLeaveEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof ServerMemberLeaveAbstract) {
                ServerMemberLeaveAbstract serverMemberLeaveAbstract = (ServerMemberLeaveAbstract) listener;

                try {
                    if (!serverMemberLeaveAbstract.onServerMemberLeave(event)) return;
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
