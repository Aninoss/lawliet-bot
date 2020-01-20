package DiscordListener;

import Constants.Permission;
import General.AutoChannel.AutoChannelContainer;
import General.PermissionCheck;
import General.AutoChannel.TempAutoChannel;
import General.PermissionCheckRuntime;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class VoiceChannelMemberLeaveListener {

    public void onLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (event.getUser().isYourself()) return;
        for (TempAutoChannel tempAutoChannel : AutoChannelContainer.getInstance().getChannelList()) {
            if (event.getChannel().getId() == tempAutoChannel.getTempChannelId()) {
                try {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(DBServer.getServerLocale(event.getServer()), "autochannel", event.getChannel(), Permission.MANAGE_CHANNEL)) {
                        if (event.getChannel().getConnectedUsers().size() == 0) {
                            AutoChannelContainer.getInstance().removeVoiceChannel(event.getChannel().getId());
                            DBServer.removeAutoChannelChildChannel(event.getChannel());
                            event.getChannel().delete().get();
                        }
                    }
                } catch (InterruptedException | ExecutionException | SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}