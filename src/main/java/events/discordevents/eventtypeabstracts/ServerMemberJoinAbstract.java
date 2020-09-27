package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

import java.util.ArrayList;

public abstract class ServerMemberJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable;

    public static void onServerMemberJoinStatic(ServerMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), true,
                listener -> ((ServerMemberJoinAbstract) listener).onServerMemberJoin(event)
        );
    }

}
