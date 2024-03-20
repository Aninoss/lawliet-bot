package events.discordevents.guildmessagereceived;

import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.FisheryEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Random;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedFishery extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());

        boolean messageRegistered = false;
        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(event.getGuild().getIdLong());
        if (!event.getMessage().getContentRaw().isEmpty() &&
                guildEntity.getFishery().getFisheryStatus() == FisheryStatus.ACTIVE &&
                !JDAUtil.collectionContainsChannelOrParent(guildEntity.getFishery().getExcludedChannelIds(), event.getChannel())
        ) {
            messageRegistered = fisheryGuildData.getMemberData(event.getMember().getIdLong())
                    .registerMessage(event.getMessage(), guildEntity);
        }

        if (!messageRegistered ||
                guildEntity.getFishery().getFisheryStatus() != FisheryStatus.ACTIVE ||
                !BotPermissionUtil.canWriteEmbed(event.getGuildChannel(), Permission.MESSAGE_HISTORY) ||
                guildEntity.getTickets().getTicketChannels().containsKey(event.getChannel().getIdLong())
        ) {
            return true;
        }

        Random r = new Random();
        FisheryEntity fisheryEntity = guildEntity.getFishery();

        if (fisheryEntity.getTreasureChests() && r.nextDouble() * 100 < fisheryEntity.getTreasureChestProbabilityInPercentEffectively()) {
            Fishery.spawnTreasureChest(event.getGuildChannel(), guildEntity);
        } else if (fisheryEntity.getPowerUps() && r.nextDouble() * 100 < fisheryEntity.getPowerUpProbabilityInPercentEffectively()) {
            Fishery.spawnPowerUp(event.getGuildChannel(), event.getMember(), guildEntity);
        }

        if (ServerPatreonBoostCache.get(event.getGuild().getIdLong()) && (fisheryEntity.getTreasureChestProbabilityInPercentEffectively() != 0.25 || fisheryEntity.getPowerUpProbabilityInPercentEffectively() != 0.35)) {
            FeatureLogger.inc(PremiumFeature.FISHERY_PROBABILITIES, event.getGuild().getIdLong());
        }

        return true;
    }

}
