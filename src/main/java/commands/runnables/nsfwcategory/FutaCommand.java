package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

@CommandProperties(
        trigger = "futa",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 3 * 60,
        requiresEmbeds = false
)
public class FutaCommand extends GelbooruAbstract {

    public FutaCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated futa";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}