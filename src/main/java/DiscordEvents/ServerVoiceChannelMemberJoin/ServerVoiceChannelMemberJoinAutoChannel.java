package DiscordEvents.ServerVoiceChannelMemberJoin;

import Commands.ManagementCategory.AutoChannelCommand;
import Constants.Permission;
import Core.DiscordApiCollection;
import Core.PermissionCheckRuntime;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerVoiceChannelMemberJoinAbstract;
import Modules.AutoChannel;
import MySQL.Modules.AutoChannel.AutoChannelBean;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@DiscordEventAnnotation
public class ServerVoiceChannelMemberJoinAutoChannel extends ServerVoiceChannelMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelMemberJoinAutoChannel.class);

    @Override
    public boolean onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) throws Throwable {
        if (!userIsConnected(event.getChannel(), event.getUser())) return true;

        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());
        if (autoChannelBean.isActive() && event.getChannel().getId() == autoChannelBean.getParentChannelId().orElse(0L)) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
            if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS) &&
                    (!event.getChannel().getCategory().isPresent() || PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), AutoChannelCommand.class, event.getChannel().getCategory().get(), Permission.MANAGE_CHANNELS_ON_SERVER))
            ) {
                int n = 1;

                for (int i = 0; i < 50; i++) {
                    if (!event.getServer().getChannelsByName(getNewVCName(autoChannelBean, event, n)).isEmpty()) n++;
                    else break;
                }

                if (!userIsConnected(event.getChannel(), event.getUser())) return true;

                //Create channel
                ServerVoiceChannelBuilder vcb = new ServerVoiceChannelBuilder(event.getServer())
                        .setName(getNewVCName(autoChannelBean, event, n))
                        .setBitrate(event.getChannel().getBitrate());
                if (event.getChannel().getCategory().isPresent())
                    vcb.setCategory(event.getChannel().getCategory().get());
                if (event.getChannel().getUserLimit().isPresent())
                    vcb.setUserlimit(event.getChannel().getUserLimit().get());
                if (autoChannelBean.isLocked())
                    vcb.setUserlimit(1);

                //Transfer permissions
                Permissions botPermission = null;
                for (Map.Entry<User, Permissions> entry : event.getChannel().getOverwrittenUserPermissions().entrySet()) {
                    if (DiscordApiCollection.getInstance().getYourself().getId() == entry.getKey().getId()) {
                        botPermission = entry.getValue();
                    }
                    vcb.addPermissionOverwrite(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<Role, Permissions> entry : event.getChannel().getOverwrittenRolePermissions().entrySet()) {
                    vcb.addPermissionOverwrite(entry.getKey(), entry.getValue());
                }

                PermissionsBuilder botPermsBuilder;
                if (botPermission == null) botPermsBuilder = new PermissionsBuilder();
                else botPermsBuilder = botPermission.toBuilder();
                botPermission = botPermsBuilder
                        .setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED)
                        .setState(PermissionType.CONNECT, PermissionState.ALLOWED)
                        .build();

                PermissionsBuilder pb = new PermissionsBuilder();
                pb.setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED);
                vcb.addPermissionOverwrite(event.getUser(), pb.build());
                vcb.addPermissionOverwrite(DiscordApiCollection.getInstance().getYourself(), botPermission);

                ServerVoiceChannel vc = vcb.create().get();
                vc.updateRawPosition(event.getChannel().getRawPosition()).get();

                try {
                    event.getUser().move(vc).get();
                } catch (Throwable e) {
                    //Ignore
                    vc.delete().get();
                    return true;
                }

                autoChannelBean.getChildChannels().add(vc.getId());
                if (!userIsConnected(vc, event.getUser())) {
                    vc.delete().get();
                    autoChannelBean.getChildChannels().remove(vc.getId());
                }
            }
        }

        return true;
    }

    private String getNewVCName(AutoChannelBean autoChannelBean, ServerVoiceChannelMemberJoinEvent event, int n) {
        String name = autoChannelBean.getNameMask();
        name = AutoChannel.resolveVariables(name, event.getChannel().getName(), String.valueOf(n), event.getUser().getDisplayName(event.getServer()));
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

    private boolean userIsConnected(ServerVoiceChannel channel, User user) {
        return user.getConnectedVoiceChannel(channel.getServer()).isPresent() && user.getConnectedVoiceChannel(channel.getServer()).get().getId() == channel.getId();
    }

}
