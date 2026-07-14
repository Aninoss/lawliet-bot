package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "give",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDF81",
        executableWithoutArgs = false,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "gift", "pay" }
)
public class GiveCommand extends Command implements FisheryInterface {

    public GiveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        MentionList<Member> memberMentioned = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember());
        ArrayList<Member> list = new ArrayList<>(memberMentioned.getList());
        list.removeIf(member -> member.getUser().isBot() || member.getIdLong() == event.getMember().getIdLong());

        if (list.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        args = memberMentioned.getFilteredArgs();
        Member user0 = event.getMember();
        Member user1 = list.get(0);
        FisheryMemberData fisheryUser0 = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(user0.getIdLong());
        FisheryMemberData fisheryUser1 = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(user1.getIdLong());
        long dailyLimitRemainder = fisheryUser1.getCoinsGiveReceivedMax() - fisheryUser1.getCoinsGiveReceived();
        long value = Math.min(MentionUtil.getAmountExt(args, fisheryUser0.getCoins()), fisheryUser0.getCoins());

        if (fisheryUser1.getCoins() >= Settings.FISHERY_MAX) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("max_coins", StringUtil.escapeMarkdown(user1.getEffectiveName()))))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
        if (getGuildEntity().getFishery().getCoinGiftLimit() && dailyLimitRemainder <= 0) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("cap_reached", StringUtil.escapeMarkdown(user1.getEffectiveName()))))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        boolean dailyLimitReached = false;
        if (getGuildEntity().getFishery().getCoinGiftLimit() && value > dailyLimitRemainder) {
            value = dailyLimitRemainder;
            dailyLimitReached = true;
        }

        boolean totalLimitReached = false;
        if (fisheryUser1.getCoins() > Settings.FISHERY_MAX - value) {
            value = Settings.FISHERY_MAX - fisheryUser1.getCoins();
            totalLimitReached = true;
        }

        if (value == -1) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (value < 1) {
            if (fisheryUser0.getCoins() <= 0) {
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("nocoins")))
                        .exceptionally(ExceptionLogger.get());
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")))
                        .exceptionally(ExceptionLogger.get());
            }
            return false;
        }

        long coins0Pre = fisheryUser0.getCoins();
        long coins1Pre = fisheryUser1.getCoins();

        fisheryUser0.addCoinsRaw(-value);
        fisheryUser1.addCoinsRaw(value);
        fisheryUser1.addCoinsGiveReceived(value);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(
                "successful",
                StringUtil.numToString(value),
                StringUtil.escapeMarkdown(user1.getEffectiveName()),
                StringUtil.escapeMarkdown(user0.getEffectiveName()),
                StringUtil.numToString(coins0Pre),
                StringUtil.numToString(coins0Pre - value),
                StringUtil.numToString(coins1Pre),
                StringUtil.numToString(coins1Pre + value)
        ));
        if (dailyLimitReached) {
            EmbedUtil.addLog(eb, LogStatus.WARNING, getString("cap_reached", StringUtil.escapeMarkdownInField(user1.getEffectiveName())));
        }
        if (totalLimitReached) {
            EmbedUtil.addLog(eb, LogStatus.WARNING, getString("max_coins", StringUtil.escapeMarkdownInField(user1.getEffectiveName())));
        }
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
