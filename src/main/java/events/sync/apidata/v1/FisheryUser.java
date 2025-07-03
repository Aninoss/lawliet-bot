package events.sync.apidata.v1;

public class FisheryUser {

    private long userId;
    private long fish;
    private long coins;
    private long dailyStreak;
    private long recentEfficiency;
    private FisheryGearLevels gearLevels;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getFish() {
        return fish;
    }

    public void setFish(long fish) {
        this.fish = fish;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(long dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    public long getRecentEfficiency() {
        return recentEfficiency;
    }

    public void setRecentEfficiency(long recentEfficiency) {
        this.recentEfficiency = recentEfficiency;
    }

    public FisheryGearLevels getGearLevels() {
        return gearLevels;
    }

    public void setGearLevels(FisheryGearLevels gearLevels) {
        this.gearLevels = gearLevels;
    }

}
