package events.discordevents.messagecreate;

import constants.FisheryStatus;
import core.CustomObservableList;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

@DiscordEvent(priority = EventPriority.LOW)
public class MessageCreateFishery extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());

        //manage message
        boolean messageRegistered = false;
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(event.getServer().get().getId());
        if (!event.getMessage().getContent().isEmpty()
                && serverBean.getFisheryStatus() == FisheryStatus.ACTIVE
                && !fisheryServerBean.getIgnoredChannelIds().contains(event.getServerTextChannel().get().getId())
        )
            messageRegistered = fisheryServerBean.getUserBean(event.getMessageAuthor().getId()).registerMessage(event.getMessage(), event.getServerTextChannel().get());

        //manage treasure chests
        if (messageRegistered &&
                new Random().nextInt(400) == 0 &&
                serverBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                serverBean.isFisheryTreasureChests() &&
                event.getChannel().canYouWrite() &&
                event.getChannel().canYouEmbedLinks() &&
                event.getChannel().canYouAddNewReactions()
        ) {
            boolean noSpamChannel = true;
            CustomObservableList<Long> ignoredChannelIds = DBFishery.getInstance().getBean(event.getServer().get().getId()).getIgnoredChannelIds();
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
