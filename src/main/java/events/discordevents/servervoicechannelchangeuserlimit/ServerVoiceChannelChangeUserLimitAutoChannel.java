package events.discordevents.servervoicechannelchangeuserlimit;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.PermissionDeprecated;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerVoiceChannelChangeUserLimitAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Locale;

@DiscordEvent
public class ServerVoiceChannelChangeUserLimitAutoChannel extends ServerVoiceChannelChangeUserLimitAbstract {

    @Override
    public boolean onServerVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewUserLimit().orElse(-1);
                    int parentUserLimit = channel.getUserLimit().orElse(-1);

                    if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                        Locale locale = serverBean.getLocale();

                        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, AutoChannelCommand.class, event.getChannel(), PermissionDeprecated.MANAGE_CHANNEL)) {
                            event.getChannel().createUpdater().setUserLimit(parentUserLimit).update().exceptionally(ExceptionLogger.get());
                        }
                    }
                });

                break;
            }
        }

        return true;
    }
}
