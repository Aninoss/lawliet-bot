package modules.osu;

import core.utils.StringUtil;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.activity.ActivityAssets;
import java.util.Optional;

public class OsuAccountCheck {

    public static Optional<String> getOsuUsernameFromActivity(Activity activity) {
        if (activity.getApplicationId().orElse(0L) == 367827983903490050L) {
            return activity.getAssets().flatMap(ActivityAssets::getLargeText).map(str -> {
                if (str.contains("("))
                    return StringUtil.trimString(str.split("\\(")[0]);
                return str;
            });
        }
        return Optional.empty();
    }

}
