package DiscordListener;

import Constants.Permission;
import General.PermissionCheckRuntime;
import MySQL.AutoChannel.AutoChannelBean;
import MySQL.AutoChannel.DBAutoChannel;
import MySQL.DBServerOld;
import MySQL.Server.DBServer;
import MySQL.Server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class VoiceChannelMemberLeaveListener {

    public void onLeave(ServerVoiceChannelMemberLeaveEvent event) throws Exception {
        if (event.getUser().isYourself()) return;

        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannels())) {
            if (event.getChannel().getId() == childChannelId) {
                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), "autochannel", event.getChannel(), Permission.MANAGE_CHANNEL)) {
                    if (event.getChannel().getConnectedUsers().size() == 0) {
                        autoChannelBean.getChildChannels().remove(childChannelId);
                        event.getChannel().delete().get();
                    }
                }
                break;
            }
        }
    }
}