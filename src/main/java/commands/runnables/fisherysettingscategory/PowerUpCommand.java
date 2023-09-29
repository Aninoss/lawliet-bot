package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Locale;

@CommandProperties(
        trigger = "powerup",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "❔",
        executableWithoutArgs = false,
        patreonRequired = true
)
public class PowerUpCommand extends FisheryMemberAccountInterface {

    public PowerUpCommand(Locale locale, String prefix) {
        super(locale, prefix, false, true, false);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        TextChannel channel = event.getTextChannel();
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getGuild(), args);
        if (!channelMention.getList().isEmpty()) {
            channel = channelMention.getList().get(0);
            args = channelMention.getFilteredArgs().trim();
        }

        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", new AtomicTextChannel(channel).getPrefixedNameInField(getLocale()));
            return EmbedFactory.getEmbedError(this, error);
        }

        int amount = 1;
        if (!args.isEmpty()) {
            if (StringUtil.stringIsInt(args)) {
                amount = Integer.parseInt(args);
                if (amount < 1 || amount > 30) {
                    return EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                    );
                }
            } else {
                return EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")
                );
            }
        }

        FeatureLogger.inc(PremiumFeature.FISHERY, event.getGuild().getIdLong());
        for (int i = 0; i < amount; i++) {
            Fishery.spawnPowerUp(channel, member, getGuildEntity());
        }

        String successText = getString(
                "success",
                amount != 1,
                StringUtil.numToString(amount),
                member.getEffectiveName(),
                new AtomicTextChannel(channel).getPrefixedNameInField(getLocale())
        );
        return EmbedFactory.getEmbedDefault(this, successText);
    }

}
