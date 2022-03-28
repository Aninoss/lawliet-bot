package events.discordevents.guildmessagereceived;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import constants.AssetIds;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.BumpReminder;
import mysql.modules.bump.DBBump;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW, allowBots = true)
public class GuildMessageReceivedAnicordBump extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID && event.getAuthor().getIdLong() == 302050872383242240L) {
            List<MessageEmbed> embedList = event.getMessage().getEmbeds();
            if (embedList.size() > 0 && embedList.get(0).getImage() != null && embedList.get(0).getDescription() != null) {
                DBBump.setNextBump(Instant.now().plus(2, ChronoUnit.HOURS));
                BumpReminder.startCountdown(2 * 60 * 60 * 1000);
            }
        }

        return true;
    }

}