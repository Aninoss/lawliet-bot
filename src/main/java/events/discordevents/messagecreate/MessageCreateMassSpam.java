package events.discordevents.messagecreate;

import constants.AssetIds;
import core.DiscordApiCollection;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateMassSpam extends MessageCreateAbstract {

    private static final HashMap<Long, ArrayList<Instant>> messages = new HashMap<>();

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == AssetIds.ANINOSS_SERVER_ID &&
                event.getChannel().getId() != 758285721877479504L &&
                event.getChannel().getId() != 462405404211675136L &&
                event.getChannel().getId() != 693912897998553198L &&
                event.getMessage().getUserAuthor().get().getJoinedAtTimestamp(event.getServer().get()).get().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            User user = event.getMessageAuthor().asUser().get();
            ArrayList<Instant> times = messages.computeIfAbsent(user.getId(), e -> new ArrayList<>());
            times.add(Instant.now());
            if (times.size() >= 4) {
                Instant firstInst = times.remove(0);
                if (firstInst.plus(5, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                    event.getServer().get().banUser(event.getMessage().getUserAuthor().get(), 1, "Anti Raid").exceptionally(ExceptionLogger.get());
                    DiscordApiCollection.getInstance().getOwner().sendMessage("ANTI RAID (SPAM) FOR " + user.getDiscriminatedName() + " IN " + event.getServerTextChannel().get().getMentionTag()).exceptionally(ExceptionLogger.get());
                    return false;
                }
            }
        }

        return true;
    }

}
