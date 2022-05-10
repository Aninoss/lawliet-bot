package events.discordevents.modalinteraction;

import java.util.Locale;
import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ModalInteractionAbstract;
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

@DiscordEvent
public class ModalInteractionTicket extends ModalInteractionAbstract {

    public static String ID = "ticket_create";

    @Override
    public boolean onModalInteraction(ModalInteractionEvent event) throws Throwable {
        if (event.getChannel() instanceof TextChannel && event.getModalId().equals(ID)) {
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            Category category = event.getTextChannel().getParentCategory();

            if (category != null && category.getTextChannels().size() < 50) {
                Ticket.createTicket(ticketData, event.getTextChannel(), event.getMember(), event.getValue("message").getAsString());
                event.deferEdit().queue();
            } else {
                Locale locale = ticketData.getGuildData().getLocale();
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(locale, TextManager.GENERAL, "rejected"))
                        .setDescription(TextManager.getString(locale, commands.Category.UTILITY, "ticket_toomanychannels"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }

        return true;
    }

}
