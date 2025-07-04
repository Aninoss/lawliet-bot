package events.sync.apidata.v1;

public class FisheryUserUpdate {

    private Long fish;
    private Long coins;
    private Long dailyStreak;
    private FisheryGearLevelsUpdate gearLevels;

    public Long getFish() {
        return fish;
    }

    public void setFish(Long fish) {
        this.fish = fish;
    }

    public Long getCoins() {
        return coins;
    }

    public void setCoins(Long coins) {
        this.coins = coins;
    }

    public Long getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(Long dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    public FisheryGearLevelsUpdate getGearLevels() {
        return gearLevels;
    }

    public void setGearLevels(FisheryGearLevelsUpdate gearLevels) {
        this.gearLevels = gearLevels;
    }

}
