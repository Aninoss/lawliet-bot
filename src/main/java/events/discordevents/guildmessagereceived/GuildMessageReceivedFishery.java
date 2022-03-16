package events.discordevents.guildmessagereceived;

import java.util.Random;
import core.CustomObservableList;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        if (event.getChannel() instanceof TextChannel) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());

            //manage message
            boolean messageRegistered = false;
            FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
            if (!event.getMessage().getContentRaw().isEmpty() &&
                    guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                    !fisheryGuildBean.getIgnoredChannelIds().contains(event.getChannel().getIdLong())
            ) {
                messageRegistered = fisheryGuildBean.getMemberData(event.getMember().getIdLong())
                        .registerMessage(event.getMessage());
            }

            //manage treasure chests
            if (messageRegistered &&
                    new Random().nextInt(400) == 0 &&
                    guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                    guildBean.isFisheryTreasureChests() &&
                    BotPermissionUtil.canWriteEmbed(event.getGuildChannel(), Permission.MESSAGE_HISTORY) &&
                    !DBTicket.getInstance().retrieve(event.getGuild().getIdLong()).getTicketChannels().containsKey(event.getChannel().getIdLong())
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
                    Fishery.spawnTreasureChest(event.getTextChannel());
                }
            }
        }

        return true;
    }

}
