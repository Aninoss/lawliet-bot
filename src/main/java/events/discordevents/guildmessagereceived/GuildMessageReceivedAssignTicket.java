package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedAssignTicket extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getChannel() instanceof TextChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketChannelEntity ticketChannelEntity = guildEntity.getTickets().getTicketChannels().get(event.getChannel().getIdLong());
            if (ticketChannelEntity != null &&
                    ticketChannelEntity.getAssignmentMode() != TicketsEntity.AssignmentMode.EVERYONE
            ) {
                Ticket.assignTicket(event.getMember(), event.getChannel().asTextChannel(), guildEntity, ticketChannelEntity);
            }
        }
        return true;
    }

}
