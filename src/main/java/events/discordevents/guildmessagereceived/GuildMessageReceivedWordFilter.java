package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.automod.WordFilter;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageReceivedWordFilter extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        return new WordFilter(event.getMessage(), guildEntity).check();
    }

}