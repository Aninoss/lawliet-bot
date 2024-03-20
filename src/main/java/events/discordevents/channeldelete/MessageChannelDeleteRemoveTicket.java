package events.discordevents.channeldelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.MessageChannelDeleteAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

@DiscordEvent
public class MessageChannelDeleteRemoveTicket extends MessageChannelDeleteAbstract {

    @Override
    public boolean onMessageChannelDelete(ChannelDeleteEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel() instanceof StandardGuildMessageChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketChannelEntity ticketChannelEntity = guildEntity.getTickets().getTicketChannels().get(event.getChannel().getIdLong());
            if (ticketChannelEntity != null) {
                Ticket.removeTicket((StandardGuildMessageChannel) event.getChannel(), guildEntity, ticketChannelEntity, null);
            }
        }
        return true;
    }

}