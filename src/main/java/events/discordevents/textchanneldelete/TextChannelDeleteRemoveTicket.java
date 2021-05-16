package events.discordevents.textchanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelDeleteAbstract;
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;

@DiscordEvent
public class TextChannelDeleteRemoveTicket extends TextChannelDeleteAbstract {

    @Override
    public boolean onTextChannelDelete(TextChannelDeleteEvent event) {
        TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());
        if (ticketChannel != null) {
            Ticket.removeTicket(event.getChannel(), ticketData, ticketChannel);
        }
        return true;
    }

}