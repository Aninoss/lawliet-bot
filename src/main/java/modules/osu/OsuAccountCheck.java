package modules.osu;

import java.util.Optional;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.RichPresence;

public class OsuAccountCheck {

    public static Optional<String> getOsuUsernameFromActivity(Activity activity) {
        RichPresence richPresence = activity.asRichPresence();
        if (richPresence != null && richPresence.getApplicationIdLong() == 367827983903490050L) {
            RichPresence.Image image = richPresence.getLargeImage();
            if (image != null && image.getText() != null) {
                String text = image.getText();
                if (image.getText().contains("(")) {
                    return Optional.of(text.split("\\(")[0].trim());
                } else {
                    return Optional.of(text);
                }
            }
        }

        return Optional.empty();
    }

}
