package DiscordListener;

import Commands.ManagementCategory.AutoChannelCommand;
import Constants.Permission;
import Core.PermissionCheckRuntime;
import MySQL.Modules.AutoChannel.AutoChannelBean;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class VoiceChannelChangeUserLimitListener {

    final static Logger LOGGER = LoggerFactory.getLogger(VoiceChannelChangeUserLimitListener.class);

    public void onVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) {
        try {
            AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());
            for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannels())) {
                if (event.getChannel().getId() == childChannelId) {
                    autoChannelBean.getParentChannel().ifPresent(channel -> {
                        int childUserLimit = event.getNewUserLimit().orElse(-1);
                        int parentUserLimit = channel.getUserLimit().orElse(-1);

                        if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                            try {
                                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                                Locale locale = serverBean.getLocale();

                                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL)) {
                                    event.getChannel().createUpdater().setUserLimit(parentUserLimit).update().get();
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                LOGGER.error("Exception", e);
                            }
                        }
                    });

                    break;
                }
            }
        } catch (ExecutionException e) {
            LOGGER.error("Exception", e);
        }
    }
}