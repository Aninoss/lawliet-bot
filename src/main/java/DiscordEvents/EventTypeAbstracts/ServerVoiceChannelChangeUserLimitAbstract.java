package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerVoiceChannelChangeUserLimitAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelChangeUserLimitAbstract.class);

    public abstract boolean onServerVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) throws Throwable;

    public static void onServerVoiceChannelChangeUserLimitStatic(ServerVoiceChannelChangeUserLimitEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerVoiceChannelChangeUserLimitAbstract) {
                ServerVoiceChannelChangeUserLimitAbstract serverVoiceChannelChangeUserLimitAbstract = (ServerVoiceChannelChangeUserLimitAbstract) listener;

                try {
                    if (!serverVoiceChannelChangeUserLimitAbstract.onServerVoiceChannelChangeUserLimit(event)) return;
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
