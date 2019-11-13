package DiscordListener;

import Commands.Management.AutoChannelCommand;
import Constants.Permission;
import General.AutoChannel.AutoChannelContainer;
import General.AutoChannel.AutoChannelData;
import General.AutoChannel.TempAutoChannel;
import General.PermissionCheckRuntime;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class VoiceChannelMemberJoinListener {
    public  VoiceChannelMemberJoinListener(){}

    private String getNewVCName(AutoChannelData autoChannelData, ServerVoiceChannelMemberJoinEvent event, int n) {
        String name = autoChannelData.getChannelName();
        name = AutoChannelCommand.replaceVariables(name, event.getChannel().getName(), String.valueOf(n), event.getUser().getDisplayName(event.getServer()));
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

    public void onJoin(ServerVoiceChannelMemberJoinEvent event) {
        if (event.getUser().isYourself() || !userIsConnected(event.getChannel(), event.getUser())) return;
        try {
            AutoChannelData autoChannelData = DBServer.getAutoChannelFromServer(event.getServer());
            if (autoChannelData.isActive() && autoChannelData.getVoiceChannel() != null && event.getChannel().getId() == autoChannelData.getVoiceChannel().getId()) {
                if (PermissionCheckRuntime.getInstance().botHasPermission(DBServer.getServerLocale(event.getServer()), "autochannel", event.getServer(), Permission.CREATE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS_ON_SERVER)) {
                    int n = 1;

                    for(int i = 0; i < 50; i++) {
                        if (!event.getServer().getChannelsByName(getNewVCName(autoChannelData, event, n)).isEmpty()) n++;
                        else break;
                    }

                    if (!userIsConnected(event.getChannel(), event.getUser())) return;

                    //Create channel
                    ServerVoiceChannelBuilder vcb = new ServerVoiceChannelBuilder(event.getServer())
                            .setName(getNewVCName(autoChannelData, event, n))
                            .setBitrate(event.getChannel().getBitrate());
                    if (event.getChannel().getCategory().isPresent())
                        vcb.setCategory(event.getChannel().getCategory().get());
                    if (event.getChannel().getUserLimit().isPresent())
                        vcb.setUserlimit(event.getChannel().getUserLimit().get());

                    //Transfer permissions
                    for(Map.Entry<User, Permissions> entry : event.getChannel().getOverwrittenUserPermissions().entrySet()) {
                        vcb.addPermissionOverwrite(entry.getKey(),entry.getValue());
                    }
                    for(Map.Entry<Role, Permissions> entry : event.getChannel().getOverwrittenRolePermissions().entrySet()) {
                        vcb.addPermissionOverwrite(entry.getKey(),entry.getValue());
                    }

                    PermissionsBuilder pb = new PermissionsBuilder();
                    pb.setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED);
                    if (autoChannelData.isCreatorCanDisconnect()) pb.setState(PermissionType.MOVE_MEMBERS, PermissionState.ALLOWED);

                    vcb.addPermissionOverwrite(event.getUser(), pb.build());

                    ServerVoiceChannel vc = vcb.create().get();

                    if (userIsConnected(event.getChannel(), event.getUser())) {
                        AutoChannelContainer.getInstance().addVoiceChannel(new TempAutoChannel(event.getChannel(), vc));
                        DBServer.addAutoChannelChildChannel(vc);
                        event.getUser().move(vc).get();
                    } else {
                        vc.delete();
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean userIsConnected(ServerVoiceChannel channel, User user) {
        return user.getConnectedVoiceChannel(channel.getServer()).isPresent() && user.getConnectedVoiceChannel(channel.getServer()).get().getId() == channel.getId();
    }

}