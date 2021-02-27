package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerVoiceChannelChangeUserLimitAbstract extends DiscordEventAbstract {

    public abstract boolean onServerVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) throws Throwable;

    public static void onServerVoiceChannelChangeUserLimitStatic(ServerVoiceChannelChangeUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, true, event.getServer().getId(),
                listener -> ((ServerVoiceChannelChangeUserLimitAbstract) listener).onServerVoiceChannelChangeUserLimit(event)
        );
    }

}
