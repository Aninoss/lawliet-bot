package events.discordevents.voicechannelupdateuserlimit;

import commands.Command;
import commands.runnables.configurationcategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelUpdateUserLimitAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.AutoChannelEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;

import java.util.Locale;

@DiscordEvent
public class VoiceChannelChangeUserLimitAutoChannel extends VoiceChannelUpdateUserLimitAbstract {

    @Override
    public boolean onVoiceChannelUpdateUserLimit(ChannelUpdateUserLimitEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        AutoChannelEntity autoChannelEntity = guildEntity.getAutoChannel();

        Long parentChannelId = autoChannelEntity.getChildChannelIdsToParentChannelId().get(event.getChannel().getIdLong());
        VoiceChannel parentChannel = event.getGuild().getVoiceChannelById(parentChannelId != null ? parentChannelId : 0L);
        if (parentChannel == null) {
            return true;
        }

        int childUserLimit = event.getNewValue();
        int parentUserLimit = parentChannel.getUserLimit();
        if (parentUserLimit != 0 && (childUserLimit == 0 || childUserLimit > parentUserLimit)) {
            Locale locale = guildEntity.getLocale();
            if (PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, (VoiceChannel) event.getChannel(), Permission.MANAGE_CHANNEL)) {
                ((VoiceChannel) event.getChannel()).getManager().setUserLimit(parentUserLimit)
                        .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
                        .queue();
            }
        }

        return true;
    }

}
