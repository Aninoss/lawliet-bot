package events.discordevents.guildmessagereceived;

import constants.FisheryStatus;
import core.CustomObservableList;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import java.util.Random;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        GuildBean guildBean = DBServer.getInstance().retrieve(event.getGuild().getIdLong());

        //manage message
        boolean messageRegistered = false;
        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        if (!event.getMessage().getContentRaw().isEmpty()
                && guildBean.getFisheryStatus() == FisheryStatus.ACTIVE
                && !fisheryGuildBean.getIgnoredChannelIds().contains(event.getChannel().getIdLong())
        )
            messageRegistered = fisheryGuildBean.getUserBean(event.getMessage().getIdLong())
                    .registerMessage(event.getMessage());

        //manage treasure chests
        if (messageRegistered &&
                new Random().nextInt(400) == 0 &&
                guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                guildBean.isFisheryTreasureChests() &&
                BotPermissionUtil.canWriteEmbed(event.getChannel(), Permission.MESSAGE_ADD_REACTION)
        ) {
            boolean noSpamChannel = true;
            CustomObservableList<Long> ignoredChannelIds = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getIgnoredChannelIds();
            for (long channelId : ignoredChannelIds) {
                if (channelId == event.getChannel().getIdLong()) {
                    noSpamChannel = false;
                    break;
                }
            }

            if (noSpamChannel) {
                Fishery.spawnTreasureChest(event.getChannel());
            }
        }

        return true;
    }

}
