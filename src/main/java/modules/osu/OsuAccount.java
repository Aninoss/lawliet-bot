package modules.osu;

public class OsuAccount {

    private long osuId;
    private String username;
    private int pp;
    private long globalRank;
    private long countryRank;
    private String avatarUrl;
    private double accuracy;
    private int level;
    private int levelProgress;

    public OsuAccount(long osuId, String username, int pp, long globalRank, long countryRank, String avatarUrl, double accuracy, int level, int levelProgress) {
        this.osuId = osuId;
        this.username = username;
        this.pp = pp;
        this.globalRank = globalRank;
        this.countryRank = countryRank;
        this.avatarUrl = avatarUrl;
        this.accuracy = accuracy;
        this.level = level;
        this.levelProgress = levelProgress;
    }

    public long getOsuId() {
        return osuId;
    }

    public String getUsername() {
        return username;
    }

    public int getPp() {
        return pp;
    }

    public long getGlobalRank() {
        return globalRank;
    }

    public long getCountryRank() {
        return countryRank;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getLevel() {
        return level;
    }

    public int getLevelProgress() {
        return levelProgress;
    }

}