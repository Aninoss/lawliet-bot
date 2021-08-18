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
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberDataCache;
import mysql.modules.fisheryusers.FisheryRecentFishGainsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "top",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFC6",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "rankings", "ranking", "rank", "ranks", "leaderboard", "t" }
)
public class TopCommand extends ListAbstract implements FisheryInterface {

    private enum OrderBy { RECENT_FISH_GAINS, FISH, COINS }

    private ArrayList<FisheryMemberDataCache> rankingSlots;
    private OrderBy orderBy;

    public TopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        rankingSlots = new ArrayList<>();
        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        Map<Long, Long> recentFishGainsMap = fisheryGuildData.getAllRecentFishGains();
        Map<Long, Long> fishMap = fisheryGuildData.getAllFish(recentFishGainsMap.keySet());
        Map<Long, Long> coinsMap = fisheryGuildData.getAllCoins(recentFishGainsMap.keySet());
        orderBy = calculateOrderBy(args.toLowerCase());
        Map<Long, Long> rankingMap = switch (orderBy) {
            case FISH -> fishMap;
            case COINS -> coinsMap;
            default -> recentFishGainsMap;
        };

        recentFishGainsMap.keySet().forEach(userId -> {
            int rank = getRank(rankingMap.values(), rankingMap.get(userId));
            rankingSlots.add(new FisheryMemberDataCache(event.getGuild().getIdLong(), userId, rank, recentFishGainsMap.get(userId), fishMap.get(userId), coinsMap.get(userId)));
        });
        rankingSlots.sort(Comparator.comparingLong(FisheryRecentFishGainsData::getRank));

        registerList(event.getMember(), rankingSlots.size(), args);
        return true;
    }

    @Override
    protected Pair<String, String> getEntry(int i) {
        FisheryMemberDataCache fisheryMemberDataCache = rankingSlots.get(i);
        Optional<Member> memberOpt = fisheryMemberDataCache.getMember();
        String userString = memberOpt
                .map(Member::getEffectiveName)
                .orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(fisheryMemberDataCache.getMemberId())));
        userString = StringUtil.escapeMarkdown(userString);

        int rank = fisheryMemberDataCache.getRank();
        String rankString = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> getString("stringrank", String.valueOf(rank));
        };

        return new Pair<>(
                Emojis.ZERO_WIDTH_SPACE + "\n" + getString(
                        "template_title",
                        rankString,
                        userString
                ),
                getString(
                        "template_descritpion",
                        Emojis.FULL_SPACE_EMOTE,
                        StringUtil.numToString(fisheryMemberDataCache.getRecentFishGains()),
                        StringUtil.numToString(fisheryMemberDataCache.getCoins()),
                        StringUtil.numToString(fisheryMemberDataCache.getFish())
                )
        );
    }

    @Override
    protected EmbedBuilder postProcessEmbed(EmbedBuilder eb) {
        return eb.setDescription(getString("desc", orderBy.ordinal(), Emojis.ZERO_WIDTH_SPACE));
    }

    private int getRank(Collection<Long> values, long ownValue) {
        return (int) (values.stream()
                        .filter(slot -> slot > ownValue)
                        .count() + 1);
    }

    private OrderBy calculateOrderBy(String args) {
        return switch (args) {
            case "fish", "fishes" -> OrderBy.FISH;
            case "coins", "coin" -> OrderBy.COINS;
            default -> OrderBy.RECENT_FISH_GAINS;
        };
    }

}