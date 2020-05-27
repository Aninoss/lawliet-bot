package DiscordEvents.MessageCreate;

import Commands.FisheryCategory.FisheryCommand;
import Commands.FisheryCategory.TreasureCommand;
import Constants.FisheryStatus;
import Constants.Settings;
import Core.CustomObservableList;
import Core.EmbedFactory;
import Core.TextManager;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;
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
