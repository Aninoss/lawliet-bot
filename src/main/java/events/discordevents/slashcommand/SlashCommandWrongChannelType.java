package events.discordevents.slashcommand;

import java.util.ArrayList;
import java.util.Locale;
import constants.Language;
import core.EmbedFactory;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.SlashCommandAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

@DiscordEvent
public class SlashCommandWrongChannelType extends SlashCommandAbstract {

    @Override
    public boolean onSlashCommand(SlashCommandInteractionEvent event, EntityManagerWrapper entityManager) {
        if (!(event.getChannel() instanceof TextChannel)) {
            Locale locale = Language.EN.getLocale();
            if (event.getGuild() != null) {
                locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
            }

            ArrayList<ActionRow> actionRowList = new ArrayList<>();
            EmbedBuilder eb = EmbedFactory.getWrongChannelTypeEmbed(locale, actionRowList);
            event.replyEmbeds(eb.build())
                    .setComponents(actionRowList)
                    .setEphemeral(true)
                    .queue();
        }

        return true;
    }

}
