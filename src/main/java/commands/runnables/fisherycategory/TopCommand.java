package commands.runnables.fisherycategory;

import java.util.*;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.ListAbstract;
import constants.Emojis;
import core.TextManager;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryRecentFishGainsData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "top",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFC6",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "rankings", "ranking", "rank", "ranks", "leaderboard", "t" }
)
public class TopCommand extends ListAbstract implements FisheryInterface {

    private ArrayList<FisheryRecentFishGainsData> rankingSlots;

    public TopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        rankingSlots = new ArrayList<>();
        Map<Long, Long> map = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getAllRecentFishGains();
        map.forEach((memberId, recentFishGains) -> {
            int rank = getRank(map.values(), recentFishGains);
            rankingSlots.add(new FisheryRecentFishGainsData(event.getGuild().getIdLong(), memberId, rank, recentFishGains));
        });
        rankingSlots.sort((s1, s2) -> Long.compare(s2.getRecentFishGains(), s1.getRecentFishGains()));
        registerList(rankingSlots.size(), args);
        return true;
    }

    @Override
    protected Pair<String, String> getEntry(int i) {
        FisheryRecentFishGainsData fisheryRecentFishGainsData = rankingSlots.get(i);
        Optional<Member> memberOpt = fisheryRecentFishGainsData.getMember();
        String userString = memberOpt
                .map(Member::getEffectiveName)
                .orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(fisheryRecentFishGainsData.getMemberId())));
        userString = StringUtil.escapeMarkdown(userString);

        int rank = fisheryRecentFishGainsData.getRank();
        String rankString = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> getString("stringrank", String.valueOf(rank));
        };

        FisheryMemberData fisheryMemberData = DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(fisheryRecentFishGainsData.getMemberId());
        return new Pair<>(
                getString(
                        "template_title",
                        rankString,
                        userString
                ),
                getString(
                        "template_descritpion",
                        Emojis.SPACEHOLDER,
                        StringUtil.numToString(fisheryRecentFishGainsData.getRecentFishGains()),
                        StringUtil.numToString(fisheryMemberData.getCoins()),
                        StringUtil.numToString(fisheryMemberData.getFish())
                ) + "\n" + Emojis.ZERO_WIDTH_SPACE
        );
    }

    private int getRank(Collection<Long> recentFishGainsCollection, long recentFishGains) {
        return (int) (recentFishGainsCollection.stream()
                        .map(slot -> slot > recentFishGains)
                        .count() + 1);
    }

}