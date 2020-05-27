package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberJoinAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelMemberJoinAbstract.class);

    public abstract boolean onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberJoinStatic(ServerVoiceChannelMemberJoinEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerVoiceChannelMemberJoinAbstract) {
                ServerVoiceChannelMemberJoinAbstract serverVoiceChannelMemberJoinAbstract = (ServerVoiceChannelMemberJoinAbstract) listener;

                try {
                    if (!serverVoiceChannelMemberJoinAbstract.onServerVoiceChannelMemberJoin(event)) return;
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
