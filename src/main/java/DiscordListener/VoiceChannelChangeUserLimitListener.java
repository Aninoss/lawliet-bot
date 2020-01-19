package DiscordListener;

import Constants.Permission;
import General.AutoChannel.AutoChannelContainer;
import General.AutoChannel.TempAutoChannel;
import General.PermissionCheckRuntime;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class VoiceChannelChangeUserLimitListener {

    public void onVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) {
        for (TempAutoChannel tempAutoChannel : AutoChannelContainer.getInstance().getChannelList()) {
            ServerVoiceChannel vc = tempAutoChannel.getTempChannel();
            if (event.getChannel().getId() == vc.getId()) {
                int childUserLimit = event.getNewUserLimit().orElse(-1);
                int parentUserLimit = tempAutoChannel.getOriginalChannel().getUserLimit().orElse(-1);

                if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                    try {
                        Locale locale = DBServer.getServerLocale(event.getServer());

                        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "autochannel", vc, Permission.MANAGE_CHANNEL)) {
                            vc.createUpdater().setUserLimit(parentUserLimit).update().get();
                        }
                    } catch (SQLException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                break;
            }
        }
    }
}