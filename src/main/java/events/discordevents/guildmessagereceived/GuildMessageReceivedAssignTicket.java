package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedAssignTicket extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getChannel() instanceof TextChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());
            if (ticketChannel != null &&
                    ticketChannel.getTicketAssignmentMode() != TicketData.TicketAssignmentMode.EVERYONE
            ) {
                Ticket.assignTicket(event.getMember(), event.getChannel().asTextChannel(), ticketData, ticketChannel, guildEntity);
            }
        }
        return true;
    }

}
