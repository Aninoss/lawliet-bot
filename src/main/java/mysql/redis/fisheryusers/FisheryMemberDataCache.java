package mysql.redis.fisheryusers;

public class FisheryMemberDataCache extends FisheryRecentFishGainsData {

    private final long fish;
    private final long coins;
    private final long dailyStreak;

    public FisheryMemberDataCache(long guildId, long memberId, int rank, long recentFishGains, long fish, long coins, long dailyStreak) {
        super(guildId, memberId, rank, recentFishGains);
        this.fish = fish;
        this.coins = coins;
        this.dailyStreak = dailyStreak;
    }

    public long getFish() {
        return fish;
    }

    public long getCoins() {
        return coins;
    }

    public long getDailyStreak() {
        return dailyStreak;
    }

}
