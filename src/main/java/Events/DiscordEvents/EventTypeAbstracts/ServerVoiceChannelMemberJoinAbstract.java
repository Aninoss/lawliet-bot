package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberJoinAbstract extends DiscordEventAbstract {

    public abstract boolean onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberJoinStatic(ServerVoiceChannelMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerVoiceChannelMemberJoinAbstract) listener).onServerVoiceChannelMemberJoin(event)
        );
    }

}
