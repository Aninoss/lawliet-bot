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
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(ChannelUpdateUserLimitEvent event) {
        AutoChannelData autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getIdLong() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewValue();
                    int parentUserLimit = channel.getUserLimit();

                    if (parentUserLimit != 0 && (childUserLimit == 0 || childUserLimit > parentUserLimit)) {
                        GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
                        Locale locale = guildBean.getLocale();

                        if (PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, (VoiceChannel) event.getChannel(), Permission.MANAGE_CHANNEL)) {
                            ((VoiceChannel) event.getChannel()).getManager().setUserLimit(parentUserLimit)
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
