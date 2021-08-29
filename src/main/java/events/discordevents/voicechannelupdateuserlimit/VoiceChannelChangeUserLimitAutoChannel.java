package events.discordevents.voicechannelupdateuserlimit;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelUpdateUserLimitAbstract;
import mysql.modules.autochannel.AutoChannelData;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) {
        AutoChannelData autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getIdLong() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewUserLimit();
                    int parentUserLimit = channel.getUserLimit();

                    if (parentUserLimit != 0 && (childUserLimit == 0 || childUserLimit > parentUserLimit)) {
                        GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
                        Locale locale = guildBean.getLocale();

                        if (PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL)) {
                            event.getChannel().getManager().setUserLimit(parentUserLimit)
                                    .reason(Command.getCommandLanguage(AutoChannelCommand.class, autoChannelBean.getGuildData().getLocale()).getTitle())
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
