package events.discordevents.guildmessagereceived;

import constants.FisheryStatus;
import core.CustomObservableList;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().get().getId());

        //manage message
        boolean messageRegistered = false;
        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(event.getServer().get().getId());
        if (!event.getMessage().getContent().isEmpty()
                && guildBean.getFisheryStatus() == FisheryStatus.ACTIVE
                && !fisheryGuildBean.getIgnoredChannelIds().contains(event.getServerTextChannel().get().getId())
        )
            messageRegistered = fisheryGuildBean.getUserBean(event.getMessageAuthor().getId()).registerMessage(event.getMessage(), event.getServerTextChannel().get());

        //manage treasure chests
        if (messageRegistered &&
                new Random().nextInt(400) == 0 &&
                guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                guildBean.isFisheryTreasureChests() &&
                event.getChannel().canYouWrite() &&
                event.getChannel().canYouEmbedLinks() &&
                event.getChannel().canYouAddNewReactions()
        ) {
            boolean noSpamChannel = true;
            CustomObservableList<Long> ignoredChannelIds = DBFishery.getInstance().retrieve(event.getServer().get().getId()).getIgnoredChannelIds();
            for (long channelId : ignoredChannelIds) {
                if (channelId == event.getChannel().getId()) {
                    noSpamChannel = false;
                    break;
                }
            }

            if (noSpamChannel) {
                Fishery.spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
            }
        }

        return true;
    }

}
