package DiscordListener.ServerVoiceChannelMemberLeave;

import Commands.ManagementCategory.AutoChannelCommand;
import Constants.Permission;
import Core.DiscordApiCollection;
import Core.PermissionCheckRuntime;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ServerVoiceChannelMemberJoinAbstract;
import DiscordListener.ListenerTypeAbstracts.ServerVoiceChannelMemberLeaveAbstract;
import MySQL.Modules.AutoChannel.AutoChannelBean;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.util.ArrayList;
import java.util.Map;

@DiscordListenerAnnotation
public class ServerVoiceChannelMemberLeaveAutoChannel extends ServerVoiceChannelMemberLeaveAbstract {

    @Override
    public boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());

        for (long childChannelId: new ArrayList<>(autoChannelBean.getChildChannels())) {
            if (event.getChannel().getId() == childChannelId) {
                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL | Permission.CONNECT)) {
                    if (event.getChannel().getConnectedUsers().size() == 0) {
                        event.getChannel().delete();
                    }
                }
                break;
            }
        }

        return true;
    }

}
