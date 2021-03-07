package events.discordevents.voicechannelupdateuserlimit;

import commands.Command;
import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.Category;
import core.PermissionCheckRuntime;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelUpdateUserLimitAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import java.util.ArrayList;
import java.util.Locale;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getIdLong() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewUserLimit();
                    int parentUserLimit = channel.getUserLimit();

                    if (parentUserLimit != 0 && (childUserLimit == 0 || childUserLimit > parentUserLimit)) {
                        GuildBean guildBean = DBServer.getInstance().retrieve(event.getGuild().getIdLong());
                        Locale locale = guildBean.getLocale();

                        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL)) {
                            event.getChannel().getManager().setUserLimit(parentUserLimit)
                                    .reason(Command.getCommandLanguage(AutoChannelCommand.class, autoChannelBean.getGuildBean().getLocale()).getTitle())
                                    .queue();
                        }
                    }
                });

                break;
            }
        }

        return true;
    }
}
