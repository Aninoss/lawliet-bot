package DiscordListener.MessageCreate;

import Commands.FisheryCategory.FisheryCommand;
import Constants.FisheryStatus;
import Constants.Settings;
import Core.CustomObservableList;
import Core.EmbedFactory;
import Core.TextManager;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;
import java.util.Random;

@DiscordListenerAnnotation()
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
                Locale locale = serverBean.getLocale();
                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_title") + Settings.EMPTY_EMOJI)
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                        .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

                Message message = event.getChannel().sendMessage(eb).get();
                message.addReaction(FisheryCommand.keyEmoji);
            }
        }

        return true;
    }

}
