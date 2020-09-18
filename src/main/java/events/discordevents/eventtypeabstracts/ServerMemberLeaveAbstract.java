package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.ArrayList;

public abstract class ServerMemberLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable;

    public static void onServerMemberLeaveStatic(ServerMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerMemberLeaveAbstract) listener).onServerMemberLeave(event)
        );
    }

}
