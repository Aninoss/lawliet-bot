package modules.osu;

import com.vdurmont.emoji.EmojiParser;

import java.util.Optional;

public class OsuAccount {

    private final long osuId;
    private final String username;
    private final String countryCode;
    private final int pp;
    private final Long globalRank;
    private final Long countryRank;
    private final String avatarUrl;
    private final double accuracy;
    private final int level;
    private final int levelProgress;

    public OsuAccount(long osuId, String username, String countryCode, int pp, Long globalRank, Long countryRank, String avatarUrl, double accuracy, int level, int levelProgress) {
        this.osuId = osuId;
        this.username = username;
        this.countryCode = countryCode;
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

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryEmoji() {
        return EmojiParser.parseToUnicode(":" + getCountryCode().toLowerCase() + ":");
    }

    public int getPp() {
        return pp;
    }

    public Optional<Long> getGlobalRank() {
        return Optional.ofNullable(globalRank);
    }

    public Optional<Long> getCountryRank() {
        return Optional.ofNullable(countryRank);
    }

    public String getAvatarUrl() {
        if (avatarUrl.startsWith("/"))
            return "https://osu.ppy.sh/" + this.avatarUrl;
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