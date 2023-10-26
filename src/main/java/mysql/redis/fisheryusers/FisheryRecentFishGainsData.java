package mysql.redis.fisheryusers;

import core.assets.MemberAsset;

public class FisheryRecentFishGainsData implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final int rank;
    private final long recentFishGains;

    public FisheryRecentFishGainsData(long guildId, long memberId, int rank, long recentFishGains) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.rank = rank;
        this.recentFishGains = recentFishGains;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public int getRank() {
        return rank;
    }

    public long getRecentFishGains() {
        return recentFishGains;
    }

}
