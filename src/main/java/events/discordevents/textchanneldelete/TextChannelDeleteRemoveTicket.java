package events.discordevents.textchanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelDeleteAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class TextChannelDeleteRemoveTicket extends TextChannelDeleteAbstract {

    @Override
    public boolean onTextChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel() instanceof TextChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketChannelEntity ticketChannelEntity = guildEntity.getTickets().getTicketChannels().get(event.getChannel().getIdLong());
            if (ticketChannelEntity != null) {
                Ticket.removeTicket(event.getChannel().asTextChannel(), guildEntity, ticketChannelEntity, null);
            }
        }
        return true;
    }

}