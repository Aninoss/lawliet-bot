package DiscordEvents.EventTypeAbstracts;

import DiscordEvents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class ServerVoiceChannelMemberLeaveAbstract extends DiscordEventAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelMemberLeaveAbstract.class);

    public abstract boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable;

    public static void onServerVoiceChannelMemberLeaveStatic(ServerVoiceChannelMemberLeaveEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if (event.getUser().isYourself()) return;

        boolean banned = userIsBanned(event.getUser().getId());

        for(DiscordEventAbstract listener : listenerList) {
            if (listener instanceof ServerVoiceChannelMemberLeaveAbstract) {
                ServerVoiceChannelMemberLeaveAbstract serverVoiceChannelMemberLeaveAbstract = (ServerVoiceChannelMemberLeaveAbstract) listener;
                if (banned && !serverVoiceChannelMemberLeaveAbstract.isAllowingBannedUser()) continue;

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
