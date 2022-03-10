package events.discordevents.guildmessagereceived;

import java.util.Random;
import modules.fishery.FisheryStatus;
import core.CustomObservableList;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.fishery.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());

        //manage message
        boolean messageRegistered = false;
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        if (!event.getMessage().getContentRaw().isEmpty() &&
                guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                !fisheryGuildBean.getIgnoredChannelIds().contains(event.getTextChannel().getIdLong())
        ) {
            messageRegistered = fisheryGuildBean.getMemberData(event.getMember().getIdLong())
                    .registerMessage(event.getMessage());
        }

        //manage treasure chests
        if (messageRegistered &&
                new Random().nextInt(400) == 0 &&
                guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                guildBean.isFisheryTreasureChests() &&
                BotPermissionUtil.canWriteEmbed(event.getTextChannel(), Permission.MESSAGE_HISTORY) &&
                !DBTicket.getInstance().retrieve(event.getGuild().getIdLong()).getTicketChannels().containsKey(event.getTextChannel().getIdLong())
        ) {
            boolean noSpamChannel = true;
            CustomObservableList<Long> ignoredChannelIds = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getIgnoredChannelIds();
            for (long channelId : ignoredChannelIds) {
                if (channelId == event.getTextChannel().getIdLong()) {
                    noSpamChannel = false;
                    break;
                }
            }

            if (noSpamChannel) {
                Fishery.spawnTreasureChest(event.getTextChannel());
            }
        }

        return true;
    }

}
