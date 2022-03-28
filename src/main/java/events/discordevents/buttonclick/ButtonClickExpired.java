package events.discordevents.buttonclick;

import java.util.Locale;
import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class ButtonClickExpired extends ButtonClickAbstract implements InteractionListenerHandler<ButtonInteractionEvent> {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event) {
        Locale locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "button_listener_expired_title"))
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "button_listener_expired_desc"));

        event.replyEmbeds(eb.build())
                .setEphemeral(true)
                .queue();
        return false;
    }

}
