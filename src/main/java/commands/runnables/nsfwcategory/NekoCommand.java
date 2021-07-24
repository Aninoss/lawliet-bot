package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "neko",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false
)
public class NekoCommand extends Rule34HentaiAbstract {

    public NekoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "cat_ears -yaoi -yuri -shemale -lesbian -gay -futa -trap";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}