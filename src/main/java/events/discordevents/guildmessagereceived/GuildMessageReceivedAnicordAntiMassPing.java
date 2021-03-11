package events.discordevents.guildmessagereceived;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import constants.AssetIds;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageReceivedAnicordAntiMassPing extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID &&
                event.getMessage().getMentionedUsers().size() >= 20 &&
                event.getMessage().getMember().hasTimeJoined() &&
                event.getMessage().getMember().getTimeJoined().toInstant().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            event.getGuild().ban(event.getMember(), 1, "Anti Raid (Mass Ping)").queue();
            event.getGuild().getTextChannelById(462420339364724751L).sendMessage("ANTI RAID (MASS PING) FOR " + event.getMember().getUser().getAsTag()).queue();
            return false;
        }

        return true;
    }

}
