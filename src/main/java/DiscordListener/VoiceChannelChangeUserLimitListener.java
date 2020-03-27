package DiscordListener;

import Constants.Permission;
import General.PermissionCheckRuntime;
import MySQL.AutoChannel.AutoChannelBean;
import MySQL.AutoChannel.DBAutoChannel;
import MySQL.DBServerOld;
import MySQL.Server.DBServer;
import MySQL.Server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class VoiceChannelChangeUserLimitListener {

    public void onVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) {
        try {
            AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getAutoChannelBean(event.getServer().getId());
            for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannels())) {
                if (event.getChannel().getId() == childChannelId) {
                    autoChannelBean.getParentChannel().ifPresent(channel -> {
                        int childUserLimit = event.getNewUserLimit().orElse(-1);
                        int parentUserLimit = channel.getUserLimit().orElse(-1);

                        if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                            try {
                                ServerBean serverBean = DBServer.getInstance().getServerBean(event.getServer().getId());
                                Locale locale = serverBean.getLocale();

                                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "autochannel", event.getChannel(), Permission.MANAGE_CHANNEL)) {
                                    event.getChannel().createUpdater().setUserLimit(parentUserLimit).update().get();
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break;
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}