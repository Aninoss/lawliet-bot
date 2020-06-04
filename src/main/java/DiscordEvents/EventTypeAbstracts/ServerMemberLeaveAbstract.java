package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerMemberLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable;

    public static void onServerMemberLeaveStatic(ServerMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        execute(event, listenerList,
                listener -> ((ServerMemberLeaveAbstract) listener).onServerMemberLeave(event)
        );
    }

}
