package modules.osu;

import java.util.Optional;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.RichPresence;

public class OsuAccountCheck {

    public static Optional<String> getOsuUsernameFromActivity(Activity activity) {
        RichPresence richPresence = activity.asRichPresence();
        if (richPresence != null && richPresence.getApplicationIdLong() == 367827983903490050L) {
            String details = richPresence.getDetails();
            if (details != null) {
                if (details.contains("(")) {
                    return Optional.of(details.split("\\(")[0].trim());
                } else {
                    return Optional.of(details);
                }
            }
        }

        return Optional.empty();
    }

}
