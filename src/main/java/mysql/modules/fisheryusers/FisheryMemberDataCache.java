package mysql.modules.fisheryusers;

public class FisheryMemberDataCache extends FisheryRecentFishGainsData {

    private final long fish;
    private final long coins;

    public FisheryMemberDataCache(long guildId, long memberId, int rank, long recentFishGains, long fish, long coins) {
        super(guildId, memberId, rank, recentFishGains);
        this.fish = fish;
        this.coins = coins;
    }

    public long getFish() {
        return fish;
    }

    public long getCoins() {
        return coins;
    }

}
