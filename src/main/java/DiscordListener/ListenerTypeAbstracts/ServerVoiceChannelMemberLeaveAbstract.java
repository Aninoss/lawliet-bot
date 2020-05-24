package DiscordListener.ListenerTypeAbstracts;

import DiscordListener.DiscordListenerAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberLeaveAbstract extends DiscordListenerAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelMemberLeaveAbstract.class);

    public abstract boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberLeaveStatic(ServerVoiceChannelMemberLeaveEvent event, ArrayList<DiscordListenerAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof ServerVoiceChannelMemberLeaveAbstract) {
                ServerVoiceChannelMemberLeaveAbstract serverVoiceChannelMemberLeaveAbstract = (ServerVoiceChannelMemberLeaveAbstract) listener;

                try {
                    if (!serverVoiceChannelMemberLeaveAbstract.onServerVoiceChannelMemberLeave(event)) return;
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
