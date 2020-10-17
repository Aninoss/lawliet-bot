package events.discordevents.servervoicechannelchangeuserlimit;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerVoiceChannelChangeUserLimitAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEvent
public class ServerVoiceChannelChangeUserLimitAutoChannel extends ServerVoiceChannelChangeUserLimitAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerVoiceChannelChangeUserLimitAutoChannel.class);

    @Override
    public boolean onServerVoiceChannelChangeUserLimit(ServerVoiceChannelChangeUserLimitEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewUserLimit().orElse(-1);
                    int parentUserLimit = channel.getUserLimit().orElse(-1);

                    if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                        try {
                            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                            Locale locale = serverBean.getLocale();

                            if (PermissionCheckRuntime.getInstance().botHasPermission(locale, AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL)) {
                                event.getChannel().createUpdater().setUserLimit(parentUserLimit).update().exceptionally(ExceptionLogger.get());
                            }
                        } catch (ExecutionException e) {
                            LOGGER.error("Exception", e);
                        }
                    }
                });

                break;
            }
        }

        return true;
    }
}
