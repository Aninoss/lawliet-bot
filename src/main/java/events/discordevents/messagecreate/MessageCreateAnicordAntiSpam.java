package events.discordevents.messagecreate;

import constants.AssetIds;
import core.RatelimitManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateAnicordAntiSpam extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == AssetIds.ANICORD_SERVER_ID &&
                event.getChannel().getId() != 758285721877479504L &&
                event.getChannel().getId() != 462405404211675136L &&
                event.getChannel().getId() != 693912897998553198L &&
                event.getMessage().getUserAuthor().get().getJoinedAtTimestamp(event.getServer().get()).get().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            User user = event.getMessageAuthor().asUser().get();
            if (RatelimitManager.getInstance().checkAndSet("aninoss_spamming", user.getId(), 3, 5, ChronoUnit.SECONDS).isPresent()) {
                event.getServer().get().banUser(event.getMessage().getUserAuthor().get(), 1, "Anti Raid (Spam)").exceptionally(ExceptionLogger.get());
                event.getServer().get().getTextChannelById(462420339364724751L).get().sendMessage("ANTI RAID (SPAM) FOR " + user.getDiscriminatedName() + " IN " + event.getServerTextChannel().get().getMentionTag()).exceptionally(ExceptionLogger.get());
                return false;
            }
        }

        return true;
    }

}
