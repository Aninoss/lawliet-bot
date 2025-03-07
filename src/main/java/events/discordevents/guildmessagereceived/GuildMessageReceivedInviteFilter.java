package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.automod.InviteFilter;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true, allowBots = true)
public class GuildMessageReceivedInviteFilter extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        return new InviteFilter(event.getMessage(), guildEntity).check();
    }

}
