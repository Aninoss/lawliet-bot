package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberLeaveStatic(ServerVoiceChannelMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, event.getUser(), true,
                listener -> ((ServerVoiceChannelMemberLeaveAbstract) listener).onServerVoiceChannelMemberLeave(event)
        );
    }

}
