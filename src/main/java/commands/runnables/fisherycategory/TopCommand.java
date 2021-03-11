package commands.runnables.fisherycategory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.ListAbstract;
import constants.Emojis;
import core.TextManager;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "top",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFC6",
        executableWithoutArgs = true,
        aliases = { "rankings", "ranking", "rank", "ranks", "leaderboard", "t" }
)
public class TopCommand extends ListAbstract implements FisheryInterface {

    private ArrayList<FisheryMemberBean> rankingSlots;

    public TopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        rankingSlots = new ArrayList<>(DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getUsers().values());
        rankingSlots.removeIf(user -> !user.isOnServer() || user.getMember().map(m -> m.getUser().isBot()).orElse(true));
        rankingSlots.sort((s1, s2) -> {
            if (s1.getFishIncome() < s2.getFishIncome()) return 1;
            if (s1.getFishIncome() > s2.getFishIncome()) return -1;
            if (s1.getFish() < s2.getFish()) return 1;
            if (s1.getFish() > s2.getFish()) return -1;
            return Long.compare(s2.getCoins(), s1.getCoins());
        });
        registerList(rankingSlots.size(), args);
        return true;
    }

    @Override
    protected Pair<String, String> getEntry(int i) {
        FisheryMemberBean memberBean = rankingSlots.get(i);
        Optional<Member> memberOpt = memberBean.getMember();
        String userString = memberOpt
                .map(Member::getEffectiveName)
                .orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(memberBean.getMemberId())));
        userString = StringUtil.escapeMarkdown(userString);

        int rank = memberBean.getRank();
        String rankString = String.valueOf(rank);
        switch (rank) {
            case 1:
                rankString = "🥇";
                break;

            case 2:
                rankString = "🥈";
                break;

            case 3:
                rankString = "🥉";
                break;

            default:
                rankString = getString("stringrank", rankString);
        }

        return new Pair<>(
                getString(
                        "template_title",
                        rankString,
                        userString
                ),
                getString(
                        "template_descritpion",
                        Emojis.SPACEHOLDER,
                        StringUtil.numToString(memberBean.getFishIncome()),
                        StringUtil.numToString(memberBean.getCoins()),
                        StringUtil.numToString(memberBean.getFish())
                )
        );
    }

}