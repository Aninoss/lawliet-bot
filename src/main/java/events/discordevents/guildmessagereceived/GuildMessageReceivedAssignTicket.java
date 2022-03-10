package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedAssignTicket extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getTextChannel().getIdLong());
        if (ticketChannel != null) {
            Ticket.assignTicket(event.getMember(), event.getTextChannel(), ticketData, ticketChannel);
        }
        return true;
    }

}
