package modules.fishery;

import java.time.Duration;
import constants.Emojis;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

public enum FisheryPowerUp {

    SHIELD(Emojis.POWERUP_SHIELD, Duration.ofHours(24)),
    LOUPE(Emojis.POWERUP_LOUPE, Duration.ofHours(24)),
    SHOP(Emojis.POWERUP_SHOP, Duration.ofHours(24)),
    TEAM(Emojis.POWERUP_TEAM, Duration.ofHours(0));

    private final CustomEmoji emoji;
    private final Duration validDuration;

    FisheryPowerUp(CustomEmoji emoji, Duration validDuration) {
        this.emoji = emoji;
        this.validDuration = validDuration;
    }

    public CustomEmoji getEmoji() {
        return emoji;
    }

    public Duration getValidDuration() {
        return validDuration;
    }

}