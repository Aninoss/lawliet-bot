package events.sync.apidata.v1;

public class FisheryUserUpdate {

    private long fish;
    private long coins;
    private long dailyStreak;
    private FisheryGearLevels gearLevels;

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

    public FisheryGearLevels getGearLevels() {
        return gearLevels;
    }

    public void setGearLevels(FisheryGearLevels gearLevels) {
        this.gearLevels = gearLevels;
    }

}
