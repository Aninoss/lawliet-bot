package modules.fishery;

import java.time.Duration;
import constants.Emojis;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

public enum FisheryPowerUp {

    SHIELD(Emojis.POWERUP_SHIELD, "https://cdn.discordapp.com/attachments/1077245845440827562/1077938510952144948/shield.png", Duration.ofHours(24)),
    LOUPE(Emojis.POWERUP_LOUPE, "https://cdn.discordapp.com/attachments/1077245845440827562/1077939019142418432/loupe.png", Duration.ofHours(24)),
    SHOP(Emojis.POWERUP_SHOP, "https://cdn.discordapp.com/attachments/1077245845440827562/1077939027686207579/shop.png", Duration.ofHours(24)),
    TEAM(Emojis.POWERUP_TEAM, "https://cdn.discordapp.com/attachments/1077245845440827562/1077939032228626492/team.png", Duration.ofHours(0));

    private final CustomEmoji emoji;
    private final String imageUrl;
    private final Duration validDuration;

    FisheryPowerUp(CustomEmoji emoji, String imageUrl, Duration validDuration) {
        this.emoji = emoji;
        this.imageUrl = imageUrl;
        this.validDuration = validDuration;
    }

    public CustomEmoji getEmoji() {
        return emoji;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Duration getValidDuration() {
        return validDuration;
    }

}