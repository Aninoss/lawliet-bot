package events.discordevents.voicechanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.VoiceChannelDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.AutoChannelEntity;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class VoiceChannelDeleteAutoChannel extends VoiceChannelDeleteAbstract {

    @Override
    public boolean onVoiceChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        AutoChannelEntity autoChannelEntity = entityManager.findGuildEntity(event.getGuild().getIdLong()).getAutoChannel();
        if (autoChannelEntity.getChildChannelIdsToParentChannelId().containsKey(event.getChannel().getIdLong())) {
            autoChannelEntity.beginTransaction();
            autoChannelEntity.getChildChannelIdsToParentChannelId().remove(event.getChannel().getIdLong());
            autoChannelEntity.commitTransaction();
        }
        return true;
    }

}
