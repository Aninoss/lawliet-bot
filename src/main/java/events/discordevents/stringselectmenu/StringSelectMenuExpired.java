package events.discordevents.stringselectmenu;

import java.util.Locale;
import core.EmbedFactory;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.StringSelectMenuAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class StringSelectMenuExpired extends StringSelectMenuAbstract {

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) {
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