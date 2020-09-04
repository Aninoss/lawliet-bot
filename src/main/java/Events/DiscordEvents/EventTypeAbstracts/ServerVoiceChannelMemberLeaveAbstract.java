package Events.DiscordEvents.EventTypeAbstracts;

import Events.DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberLeaveAbstract extends DiscordEventAbstract {

    public abstract boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberLeaveStatic(ServerVoiceChannelMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(event, listenerList,
                listener -> ((ServerVoiceChannelMemberLeaveAbstract) listener).onServerVoiceChannelMemberLeave(event)
        );
    }

}
