package events.discordevents.guildmessagereceived;

import java.util.Random;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getChannel() instanceof TextChannel) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());

            //manage message
            boolean messageRegistered = false;
            FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
            if (!event.getMessage().getContentRaw().isEmpty() &&
                    guildBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                    !fisheryGuildBean.getIgnoredChannelIds().contains(event.getChannel().getIdLong())
            ) {
                messageRegistered = fisheryGuildBean.getMemberData(event.getMember().getIdLong())
                        .registerMessage(event.getMessage(), guildEntity);
            }

            if (!messageRegistered ||
                    guildBean.getFisheryStatus() != FisheryStatus.ACTIVE ||
                    !BotPermissionUtil.canWriteEmbed(event.getGuildChannel(), Permission.MESSAGE_HISTORY) ||
                    DBTicket.getInstance().retrieve(event.getGuild().getIdLong()).getTicketChannels().containsKey(event.getChannel().getIdLong())
            ) {
                return true;
            }

            //manage treasure chests and power-ups
            Random r = new Random();
            if (guildBean.isFisheryTreasureChests() && r.nextInt(400) == 0) {
                if (isNotASpamChannel(event.getGuildChannel())) {
                    Fishery.spawnTreasureChest(event.getChannel().asTextChannel(), guildEntity);
                }
            } else if (guildBean.isFisheryPowerups() && r.nextInt(300) == 0) {
                if (isNotASpamChannel(event.getGuildChannel())) {
                    Fishery.spawnPowerUp(event.getChannel().asTextChannel(), event.getMember(), guildEntity);
                }
            }
        }

        return true;
    }

    private boolean isNotASpamChannel(GuildMessageChannelUnion channel) {
        return DBFishery.getInstance().retrieve(channel.getGuild().getIdLong())
                .getIgnoredChannelIds()
                .stream()
                .noneMatch(channelId -> channelId == channel.getIdLong());
    }

}
