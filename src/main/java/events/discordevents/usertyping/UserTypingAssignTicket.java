package events.discordevents.usertyping;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserTypingAbstract;
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.events.user.UserTypingEvent;

@DiscordEvent
public class UserTypingAssignTicket extends UserTypingAbstract {

    @Override
    public boolean onUserTyping(UserTypingEvent event) throws Throwable {
        TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());
        if (ticketChannel != null) {
            Ticket.assignTicket(event.getMember(), event.getTextChannel(), ticketData, ticketChannel);
        }
        return true;
    }

}
