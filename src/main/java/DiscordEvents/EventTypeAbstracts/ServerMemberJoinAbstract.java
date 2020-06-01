package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerMemberJoinAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinAbstract.class);

    public abstract boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable;

    public static void onServerMemberJoinStatic(ServerMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerMemberJoinAbstract) {
                ServerMemberJoinAbstract serverMemberJoinAbstract = (ServerMemberJoinAbstract) listener;
                if (banned && !serverMemberJoinAbstract.isAllowingBannedUser()) continue;

                try {
                    if (!serverMemberJoinAbstract.onServerMemberJoin(event)) return;
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
