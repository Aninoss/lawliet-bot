package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerMemberLeaveAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeaveAbstract.class);

    public abstract boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable;

    public static void onServerMemberLeaveStatic(ServerMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerMemberLeaveAbstract) {
                ServerMemberLeaveAbstract serverMemberLeaveAbstract = (ServerMemberLeaveAbstract) listener;
                if (banned && !serverMemberLeaveAbstract.isAllowingBannedUser()) continue;

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
