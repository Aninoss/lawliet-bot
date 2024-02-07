package events.discordevents.modalinteraction;

import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ModalInteractionAbstract;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

@DiscordEvent
public class ModalInteractionTicket extends ModalInteractionAbstract {

    public static String ID = "ticket_create";

    @Override
    public boolean onModalInteraction(ModalInteractionEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannel() instanceof TextChannel && event.getModalId().equals(ID)) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            Category category = event.getChannel().asTextChannel().getParentCategory();

            if (category == null || category.getTextChannels().size() < 50) {
                Ticket.createTicket(ticketData, guildEntity, event.getChannel().asTextChannel(), event.getMember(),
                        event.getValue("message").getAsString()
                );
                event.deferEdit().queue();
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "rejected"))
                        .setDescription(TextManager.getString(guildEntity.getLocale(), commands.Category.CONFIGURATION, "ticket_toomanychannels"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }

            return false;
        }

        return true;
    }

}
