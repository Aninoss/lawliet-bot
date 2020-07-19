package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberLeaveAbstract extends DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelMemberLeaveAbstract.class);

    public abstract boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberLeaveStatic(ServerVoiceChannelMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        execute(event, listenerList,
                listener -> ((ServerVoiceChannelMemberLeaveAbstract) listener).onServerVoiceChannelMemberLeave(event)
        );
    }

}
