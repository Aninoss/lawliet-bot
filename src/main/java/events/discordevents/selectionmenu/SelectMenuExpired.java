package events.discordevents.selectionmenu;

import java.util.Locale;
import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.SelectMenuAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class SelectMenuExpired extends SelectMenuAbstract implements InteractionListenerHandler<SelectMenuInteractionEvent> {

    @Override
    public boolean onSelectMenu(SelectMenuInteractionEvent event) {
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
