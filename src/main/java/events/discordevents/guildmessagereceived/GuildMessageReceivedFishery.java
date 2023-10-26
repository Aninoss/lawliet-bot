package events.discordevents.guildmessagereceived;

import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.modules.ticket.DBTicket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Random;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getChannel() instanceof TextChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());

            boolean messageRegistered = false;
            FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(event.getGuild().getIdLong());
            if (!event.getMessage().getContentRaw().isEmpty() &&
                    guildEntity.getFishery().getFisheryStatus() == FisheryStatus.ACTIVE &&
                    !guildEntity.getFishery().getExcludedChannelIds().contains(event.getChannel().getIdLong())
            ) {
                messageRegistered = fisheryGuildData.getMemberData(event.getMember().getIdLong())
                        .registerMessage(event.getMessage(), guildEntity);
            }

            if (!messageRegistered ||
                    guildEntity.getFishery().getFisheryStatus() != FisheryStatus.ACTIVE ||
                    !BotPermissionUtil.canWriteEmbed(event.getGuildChannel(), Permission.MESSAGE_HISTORY) ||
                    DBTicket.getInstance().retrieve(event.getGuild().getIdLong()).getTicketChannels().containsKey(event.getChannel().getIdLong())
            ) {
                return true;
            }

            Random r = new Random();
            if (guildEntity.getFishery().getTreasureChests() && r.nextInt(400) == 0) {
                Fishery.spawnTreasureChest(event.getChannel().asTextChannel(), guildEntity);
            } else if (guildEntity.getFishery().getPowerUps() && r.nextInt(300) == 0) {
                Fishery.spawnPowerUp(event.getChannel().asTextChannel(), event.getMember(), guildEntity);
            }
        }

        return true;
    }

}
