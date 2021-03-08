package commands.runnables.fisherycategory;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "give",
        botPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDF81",
        executableWithoutArgs = false,
        aliases = {"gift", "pay" }
)
public class GiveCommand extends Command implements FisheryInterface {

    public GiveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        Message message = event.getMessage();
        MentionList<Member> memberMentioned = MentionUtil.getMembers(message, args);
        ArrayList<Member> list = memberMentioned.getList();
        list.removeIf(member -> member.getUser().isBot() || member.getIdLong() == event.getMember().getIdLong());

        if (list.size() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("no_mentions")).build()).queue();
            return false;
        }

        args = memberMentioned.getResultMessageString();

        Member user0 = event.getMember();
        Member user1 = list.get(0);

        FisheryMemberBean fisheryUser0 = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(user0.getIdLong());
        FisheryMemberBean fisheryUser1 = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(user1.getIdLong());
        long value = Math.min(MentionUtil.getAmountExt(args, fisheryUser0.getCoins()), fisheryUser0.getCoins());
        long cap = fisheryUser1.getCoinsGivenMax() - fisheryUser1.getCoinsGiven();

        boolean limitCapped = false;
        if (fisheryUser0.getGuildBean().hasFisheryCoinsGivenLimit() && value >= cap) {
            if (cap > 0) {
                value = cap;
                limitCapped = true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("cap_reached", StringUtil.escapeMarkdown(user1.getEffectiveName()))).build())
                        .queue();
                return false;
            }
        }

        if (value != -1) {
            if (value >= 1) {
                long coins0Pre = fisheryUser0.getCoins();
                long coins1Pre = fisheryUser1.getCoins();

                fisheryUser0.addCoinsRaw(-value);
                fisheryUser1.addCoinsRaw(value);
                fisheryUser1.addCoinsGiven(value);

                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("successful",
                        StringUtil.numToString(value),
                        user1.getAsMention(),
                        user0.getAsMention(),
                        StringUtil.numToString(coins0Pre),
                        StringUtil.numToString(coins0Pre - value),
                        StringUtil.numToString(coins1Pre),
                        StringUtil.numToString(coins1Pre + value)
                ));

                if (limitCapped)
                    EmbedUtil.addLog(eb, LogStatus.WARNING, getString("cap_reached", StringUtil.escapeMarkdownInField(user1.getEffectiveName())));

                event.getChannel().sendMessage(eb.build()).queue();
                return true;
            } else {
                if (fisheryUser0.getCoins() <= 0) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("nocoins")).build())
                            .queue();
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")).build())
                            .queue();
                }
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")).build())
                    .queue();
        }

        return false;
    }
}
