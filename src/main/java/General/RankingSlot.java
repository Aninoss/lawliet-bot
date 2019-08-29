package General;

import org.javacord.api.entity.user.User;

public class RankingSlot {
    private int rank;
    private long joule, coins, growth, userId;;
    private User user;

    public RankingSlot(int rank, long joule, long coins, long growth, User user, long userId) {
        this.rank = rank;
        this.joule = joule;
        this.coins = coins;
        this.user = user;
        this.growth = growth;
        this.userId = userId;
    }

    public int getRank() {
        return rank;
    }

    public long getJoule() {
        return joule;
    }

    public long getCoins() {
        return coins;
    }

    public User getUser() {
        return user;
    }

    public long getUserId() {
        return userId;
    }

    public long getGrowth() {
        return growth;
    }
}
