package events.discordevents.messagecreate;

import constants.AssetIds;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateAnicordAntiMassPing extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == AssetIds.ANICORD_SERVER_ID &&
                event.getMessage().getMentionedUsers().size() >= 20 &&
                event.getMessage().getUserAuthor().get().getJoinedAtTimestamp(event.getServer().get()).get().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            event.getServer().get().banUser(event.getMessage().getUserAuthor().get(), 1, "Anti Raid (Mass Ping)").exceptionally(ExceptionLogger.get());
            event.getServer().get().getTextChannelById(462420339364724751L).get().sendMessage("ANTI RAID (MASS PING) FOR " + event.getMessage().getUserAuthor().get().getDiscriminatedName()).exceptionally(ExceptionLogger.get());
            return false;
        }

        return true;
    }

}
