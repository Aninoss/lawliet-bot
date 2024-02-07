package events.discordevents.voicechannelupdateuserlimit;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.runnables.configurationcategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelUpdateUserLimitAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.autochannel.AutoChannelData;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(ChannelUpdateUserLimitEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        AutoChannelData autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getIdLong() == childChannelId) {
                autoChannelBean.getParentChannel().ifPresent(channel -> {
                    int childUserLimit = event.getNewValue();
                    int parentUserLimit = channel.getUserLimit();

                    if (parentUserLimit != 0 && (childUserLimit == 0 || childUserLimit > parentUserLimit)) {
                        Locale locale = guildEntity.getLocale();
                        if (PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, (VoiceChannel) event.getChannel(), Permission.MANAGE_CHANNEL)) {
                            ((VoiceChannel) event.getChannel()).getManager().setUserLimit(parentUserLimit)
                                    .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
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
