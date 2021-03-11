package events.discordevents.guildmessagereceived;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import constants.AssetIds;
import core.RatelimitManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageReceivedAnicordAntiSpam extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID &&
                event.getChannel().getIdLong() != 758285721877479504L &&
                event.getChannel().getIdLong() != 462405404211675136L &&
                event.getChannel().getIdLong() != 693912897998553198L &&
                event.getMember().hasTimeJoined() &&
                event.getMember().getTimeJoined().toInstant().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            if (RatelimitManager.getInstance().checkAndSet("anicord_spamming", event.getMember().getId(), 3, Duration.ofSeconds(3)).isPresent()) {
                event.getGuild().ban(event.getMember(), 1, "Anti Raid (Spam)").queue();
                event.getGuild().getTextChannelById(462420339364724751L).sendMessage("ANTI RAID (SPAM) FOR " + event.getMember().getUser().getAsTag() + " IN " + event.getChannel().getAsMention()).queue();
                return false;
            }
        }

        return true;
    }

}
