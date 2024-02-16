package events.discordevents.usertyping;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.UserTypingAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import net.dv8tion.jda.api.events.user.UserTypingEvent;

@DiscordEvent
public class UserTypingAssignTicket extends UserTypingAbstract {

    @Override
    public boolean onUserTyping(UserTypingEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        TicketChannelEntity ticketChannelEntity = guildEntity.getTickets().getTicketChannels().get(event.getChannel().getIdLong());
        if (ticketChannelEntity != null &&
                ticketChannelEntity.getAssignmentMode() == TicketsEntity.AssignmentMode.FIRST &&
                !ticketChannelEntity.getAssigned()
        ) {
            Ticket.assignTicket(event.getMember(), event.getChannel().asTextChannel(), guildEntity, ticketChannelEntity);
        }
        return true;
    }

}
