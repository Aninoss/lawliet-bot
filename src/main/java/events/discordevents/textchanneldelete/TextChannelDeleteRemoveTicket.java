package events.discordevents.textchanneldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.TextChannelDeleteAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class TextChannelDeleteRemoveTicket extends TextChannelDeleteAbstract {

    @Override
    public boolean onTextChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel() instanceof TextChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());
            if (ticketChannel != null) {
                Ticket.removeTicket(event.getChannel().asTextChannel(), ticketData, ticketChannel, guildEntity);
            }
        }
        return true;
    }

}