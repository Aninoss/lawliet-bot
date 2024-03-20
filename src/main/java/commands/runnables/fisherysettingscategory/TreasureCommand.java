package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.Locale;

@CommandProperties(
        trigger = "treasure",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "tresure", "treasurechest" }
)
public class TreasureCommand extends Command implements FisheryInterface {

    public TreasureCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) {
        GuildMessageChannel channel = event.getMessageChannel();
        MentionList<GuildMessageChannel> channelMention = MentionUtil.getGuildMessageChannels(event.getGuild(), args);
        if (!channelMention.getList().isEmpty()) {
            channel = channelMention.getList().get(0);
            args = channelMention.getFilteredArgs().trim();
        }

        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale()));
            drawMessageNew(EmbedFactory.getEmbedError(this, error)).exceptionally(ExceptionLogger.get());
            return false;
        }

        int amount = 1;
        if (!args.isEmpty()) {
            if (StringUtil.stringIsInt(args)) {
                amount = Integer.parseInt(args);
                if (amount < 1 || amount > 30) {
                    drawMessageNew(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                    )).exceptionally(ExceptionLogger.get());
                    return false;
                }
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")
                )).exceptionally(ExceptionLogger.get());
                return false;
            }
        }

        FeatureLogger.inc(PremiumFeature.FISHERY_SPAWN, event.getGuild().getIdLong());
        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        BotLogEntity.log(entityManager, BotLogEntity.Event.FISHERY_SPAWN_TREASURE_CHEST, event.getMember(), channel.getIdLong());
        entityManager.getTransaction().commit();

        for (int i = 0; i < amount; i++) {
            Fishery.spawnTreasureChest(channel, getGuildEntity());
        }
        drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("success", amount != 1, StringUtil.numToString(amount), new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale()))));
        return true;
    }

}
