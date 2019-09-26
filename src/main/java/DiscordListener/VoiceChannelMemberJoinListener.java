package DiscordListener;

import Commands.ServerManagement.AutoChannelCommand;
import Constants.Permission;
import General.AutoChannel.AutoChannelContainer;
import General.AutoChannel.AutoChannelData;
import General.PermissionCheck;
import General.AutoChannel.TempAutoChannel;
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
        return AutoChannelCommand.replaceVariables(name, event.getChannel().getName(), String.valueOf(n), event.getUser().getDisplayName(event.getServer()));
    }

    public void onJoin(ServerVoiceChannelMemberJoinEvent event) {
        if (event.getUser().isYourself() || !event.getChannel().getConnectedUsers().contains(event.getUser())) return;
        try {
            AutoChannelData autoChannelData = DBServer.getAutoChannelFromServer(event.getServer());
            if (autoChannelData.isActive() && event.getChannel().equals(autoChannelData.getVoiceChannel())) {
                //Überprüft Berechtigungen des Bots
                if (PermissionCheck.getMissingPermissionListForUser(event.getServer(), event.getChannel(),event.getApi().getYourself(), Permission.CREATE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS_ON_SERVER).size() == 0) {
                    //Bestimmt Channel Nr.
                    int n = 1;

                    for(int i = 0; i < 50; i++) {
                        if (!event.getServer().getChannelsByName(getNewVCName(autoChannelData, event, n)).isEmpty()) n++;
                        else break;
                    }

                    //Erstellt Channel
                    ServerVoiceChannelBuilder vcb = new ServerVoiceChannelBuilder(event.getServer())
                            .setName(getNewVCName(autoChannelData, event, n))
                            .setBitrate(event.getChannel().getBitrate());
                    if (event.getChannel().getCategory().isPresent())
                        vcb.setCategory(event.getChannel().getCategory().get());
                    if (event.getChannel().getUserLimit().isPresent())
                        vcb.setUserlimit(event.getChannel().getUserLimit().get());

                    //Überträgt Berechtigungen
                    for(Map.Entry<User, Permissions> entry : event.getChannel().getOverwrittenUserPermissions().entrySet()) {
                        vcb.addPermissionOverwrite(entry.getKey(),entry.getValue());
                    }
                    for(Map.Entry<Role, Permissions> entry : event.getChannel().getOverwrittenRolePermissions().entrySet()) {
                        vcb.addPermissionOverwrite(entry.getKey(),entry.getValue());
                    }
                    vcb.addPermissionOverwrite(event.getUser(), new PermissionsBuilder().setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED).build());

                    ServerVoiceChannel vc = vcb.create().get();
                    AutoChannelContainer.getInstance().addVoiceChannel(new TempAutoChannel(event.getChannel(), vc));

                    DBServer.addAutoChannelChildChannel(vc);

                    //Verschiebt den User in den neuen VC
                    event.getUser().move(vc).get();
                }
            }
        } catch (InterruptedException | ExecutionException | SQLException e) {
            e.printStackTrace();
        }
    }
}