package events.discordevents.modalinteraction;

import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.ModalInteractionAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.util.Locale;

@DiscordEvent(priority = EventPriority.LOW)
public class ModalInteractionExpired extends ModalInteractionAbstract {

    @Override
    public boolean onModalInteraction(ModalInteractionEvent event, EntityManagerWrapper entityManager) {
        Locale locale = entityManager.findGuildEntity(event.getGuild().getIdLong()).getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "button_listener_expired_title"))
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "button_listener_expired_desc"));

        event.replyEmbeds(eb.build())
                .setEphemeral(true)
                .queue();
        return false;
    }

}
