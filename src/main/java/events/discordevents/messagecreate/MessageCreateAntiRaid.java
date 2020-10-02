package events.discordevents.messagecreate;

import constants.AssetIds;
import core.DiscordApiCollection;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class MessageCreateAntiRaid extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        final long ANINOSS_SERVER_ID = AssetIds.ANINOSS_SERVER_ID;

        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == ANINOSS_SERVER_ID && event.getMessage().getMentionedUsers().size() >= 30) {
            event.getServer().get().banUser(event.getMessage().getUserAuthor().get(), 1, "Anti Raid").exceptionally(ExceptionLogger.get());
            DiscordApiCollection.getInstance().getOwner().sendMessage("ANTI RAID FOR " + event.getMessage().getUserAuthor().get().getDiscriminatedName()).exceptionally(ExceptionLogger.get());
            return false;
        }

        return true;
    }

}
