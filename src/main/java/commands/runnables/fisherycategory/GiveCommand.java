package commands.runnables.fisherycategory;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

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

        if (list.size() == 0) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        args = memberMentioned.getFilteredArgs();

        Member user0 = event.getMember();
        Member user1 = list.get(0);

        FisheryMemberData fisheryUser0 = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(user0.getIdLong());
        FisheryMemberData fisheryUser1 = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(user1.getIdLong());
        long value = Math.min(MentionUtil.getAmountExt(args, fisheryUser0.getCoins()), fisheryUser0.getCoins());
        long cap = fisheryUser1.getCoinsGiveReceivedMax() - fisheryUser1.getCoinsGiveReceived();

        boolean limitCapped = false;
        if (fisheryUser0.getGuildData().hasFisheryCoinsGivenLimit() && value >= cap) {
            if (cap > 0) {
                value = cap;
                limitCapped = true;
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("cap_reached", StringUtil.escapeMarkdown(user1.getEffectiveName()))))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        }

        if (value != -1) {
            if (value >= 1) {
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

                if (limitCapped) {
                    EmbedUtil.addLog(eb, LogStatus.WARNING, getString("cap_reached", StringUtil.escapeMarkdownInField(user1.getEffectiveName())));
                }

                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return true;
            } else {
                if (fisheryUser0.getCoins() <= 0) {
                    drawMessageNew(EmbedFactory.getEmbedError(this, getString("nocoins")))
                            .exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")))
                            .exceptionally(ExceptionLogger.get());
                }
            }
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")))
                    .exceptionally(ExceptionLogger.get());
        }

        return false;
    }

}
