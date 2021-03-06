package events.discordevents.voicechannelupdateuserlimit;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.PermissionDeprecated;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelUpdateUserLimitAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelChangeUserLimitEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Locale;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(ServerVoiceChannelChangeUserLimitEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getServer().getId());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewUserLimit().orElse(-1);
                    int parentUserLimit = channel.getUserLimit().orElse(-1);

                    if (parentUserLimit != -1 && (childUserLimit == -1 || childUserLimit > parentUserLimit)) {
                        GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().getId());
                        Locale locale = guildBean.getLocale();

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
