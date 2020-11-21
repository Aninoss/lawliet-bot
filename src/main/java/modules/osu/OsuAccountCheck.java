package modules.osu;

import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.activity.ActivityAssets;

import java.util.Optional;

public class OsuAccountCheck {

    public static Optional<String> getOsuUsernameFromActivity(Activity activity) {
        if (activity.getApplicationId().orElse(0L) == 367827983903490050L) {
            return activity.getAssets().flatMap(ActivityAssets::getLargeText);
        }
        return Optional.empty();
    }

}
