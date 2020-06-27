package DiscordEvents.MessageCreate;

import Commands.FisherySettingsCategory.TreasureCommand;
import Constants.FisheryStatus;
import Core.CustomObservableList;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

@DiscordEventAnnotation()
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
                TreasureCommand.spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
            }
        }

        return true;
    }

}
