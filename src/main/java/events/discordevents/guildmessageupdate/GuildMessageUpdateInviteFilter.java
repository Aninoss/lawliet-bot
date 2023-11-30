package events.discordevents.guildmessageupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageUpdateAbstract;
import modules.automod.InviteFilter;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageUpdateInviteFilter extends GuildMessageUpdateAbstract {

    @Override
    public boolean onGuildMessageUpdate(MessageUpdateEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        return new InviteFilter(event.getMessage(), guildEntity).check();
    }

}
