package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerMemberJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable;

    public static void onServerMemberJoinStatic(ServerMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        execute(event, listenerList,
                listener -> ((ServerMemberJoinAbstract) listener).onServerMemberJoin(event)
        );
    }

}
