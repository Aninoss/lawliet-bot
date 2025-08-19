package events.discordevents.channeldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.MessageChannelDeleteAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class MessageChannelDeleteDeleteLock extends MessageChannelDeleteAbstract {

    @Override
    public boolean onMessageChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        if (guildEntity.getChannelLocks().containsKey(event.getChannel().getIdLong())) {
            guildEntity.beginTransaction();
            guildEntity.getChannelLocks().remove(event.getChannel().getIdLong());
            guildEntity.commitTransaction();
        }
        return true;
    }

}