package DiscordListener;

import Constants.Permission;
import General.AutoChannel.AutoChannelContainer;
import General.PermissionCheck;
import General.AutoChannel.TempAutoChannel;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.util.concurrent.ExecutionException;

public class VoiceChannelMemberLeaveListener {
    public VoiceChannelMemberLeaveListener(){}

    public void onLeave(ServerVoiceChannelMemberLeaveEvent event) {
        if (event.getUser().isYourself()) return;
        try {
            for (TempAutoChannel tempAutoChannel: AutoChannelContainer.getInstance().getChannelList()) {
                ServerVoiceChannel vc = tempAutoChannel.getTempChannel();
                if (event.getChannel().getId() == vc.getId()) {
                    try {
                        //Überprüft Berechtigungen des Bots
                        if (PermissionCheck.getMissingPermissionListForUser(event.getServer(), event.getChannel(),event.getApi().getYourself(), Permission.CREATE_CHANNELS_ON_SERVER).size() == 0) {
                            //Löscht Channel
                            if (event.getChannel().getConnectedUsers().size() == 0) {
                                vc.delete().get();
                                AutoChannelContainer.getInstance().removeVoiceChannel(vc);
                                DBServer.removeAutoChannelChildChannel(vc);
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}