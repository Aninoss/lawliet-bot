package commands.runnables.fisherycategory;

import java.util.*;
import commands.CommandEvent;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

@CommandProperties(
        trigger = "top",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFC6",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "rankings", "ranking", "rank", "ranks", "t", "fisherytop", "fishtop" }
)
public class TopCommand extends ListAbstract implements FisheryInterface {

    private enum OrderBy { RECENT_FISH_GAINS, FISH, COINS, DAILY_STREAK }

    private ArrayList<FisheryMemberDataCache> rankingSlots;

    public TopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        registerList(event.getMember(), args, getString("values").split("\n"));
        return true;
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        rankingSlots = new ArrayList<>();
        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(member.getGuild().getIdLong());
        Map<Long, Long> recentFishGainsMap = fisheryGuildData.getAllRecentFishGains();
        Map<Long, Long> fishMap = Collections.emptyMap();
        Map<Long, Long> coinsMap = Collections.emptyMap();
        Map<Long, Long> dailyStreakMap = Collections.emptyMap();
        Map<Long, Long> rankingMap;
        switch (OrderBy.values()[orderBy]) {
            case FISH -> {
                fishMap = fisheryGuildData.getAllFish(recentFishGainsMap.keySet());
                rankingMap = fishMap;
            }
            case COINS -> {
                coinsMap = fisheryGuildData.getAllCoins(recentFishGainsMap.keySet());
                rankingMap = coinsMap;
            }
            case DAILY_STREAK -> {
                dailyStreakMap = fisheryGuildData.getAllDailyStreaks(recentFishGainsMap.keySet());
                rankingMap = dailyStreakMap;
            }
            default -> {
                fishMap = fisheryGuildData.getAllFish(recentFishGainsMap.keySet());
                coinsMap = fisheryGuildData.getAllCoins(recentFishGainsMap.keySet());
                rankingMap = recentFishGainsMap;
            }
        }

        for (Long userId : recentFishGainsMap.keySet()) {
            int rank = getRank(rankingMap.values(), rankingMap.get(userId));
            FisheryMemberDataCache fisheryMemberDataCache = new FisheryMemberDataCache(member.getGuild().getIdLong(),
                    userId, rank, recentFishGainsMap.get(userId), fishMap.getOrDefault(userId, 0L),
                    coinsMap.getOrDefault(userId, 0L), dailyStreakMap.getOrDefault(userId, 0L));
            rankingSlots.add(fisheryMemberDataCache);
        }
        rankingSlots.sort(Comparator.comparingLong(FisheryRecentFishGainsData::getRank));
        return rankingSlots.size();
    }

    @Override
    protected Pair<String, String> getEntry(int i, int orderBy) {
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
                getEntryValue(fisheryMemberDataCache, OrderBy.values()[orderBy])
        );
    }

    private String getEntryValue(FisheryMemberDataCache fisheryMemberDataCache, OrderBy orderBy) {
        FisheryProperty[] fisheryProperties = new FisheryProperty[]{
                new FisheryProperty(Emojis.GROWTH, fisheryMemberDataCache.getRecentFishGains(), orderBy == OrderBy.RECENT_FISH_GAINS),
                new FisheryProperty(Emojis.FISH, fisheryMemberDataCache.getFish(), orderBy == OrderBy.FISH || orderBy == OrderBy.RECENT_FISH_GAINS),
                new FisheryProperty(Emojis.COINS, fisheryMemberDataCache.getCoins(), orderBy == OrderBy.COINS || orderBy == OrderBy.RECENT_FISH_GAINS),
                new FisheryProperty(Emojis.DAILY_STREAK, fisheryMemberDataCache.getDailyStreak(), orderBy == OrderBy.DAILY_STREAK)
        };
        StringBuilder sb = new StringBuilder();
        for (FisheryProperty fisheryProperty : fisheryProperties) {
            if (fisheryProperty.show) {
                if (sb.isEmpty()) {
                    sb.append(Emojis.FULL_SPACE_UNICODE).append(Emojis.FULL_SPACE_UNICODE).append(" ");
                } else {
                    sb.append("⠀⠀");
                }
                sb.append(fisheryProperty.emoji)
                        .append(" **")
                        .append(StringUtil.numToString(fisheryProperty.value))
                        .append("**");
            }
        }
        return sb.toString();
    }

    private int getRank(Collection<Long> values, long ownValue) {
        return (int) (values.stream()
                        .filter(slot -> slot > ownValue)
                        .count() + 1);
    }

    protected int calculateOrderBy(String args) {
        return switch (args) {
            case "gains", "recent", "recentfish", "recentgains" -> OrderBy.RECENT_FISH_GAINS.ordinal();
            case "fish", "fishes" -> OrderBy.FISH.ordinal();
            case "coins", "coin" -> OrderBy.COINS.ordinal();
            case "daily", "daily_streak", "dailystreak", "streak" -> OrderBy.DAILY_STREAK.ordinal();
            default -> -1;
        };
    }


    private static class FisheryProperty {

        private final String emoji;
        private final Long value;
        private final boolean show;

        public FisheryProperty(String emoji, Long value, boolean show) {
            this.emoji = emoji;
            this.value = value;
            this.show = show;
        }

    }

}