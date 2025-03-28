package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.E621Abstract;

@CommandProperties(
        trigger = "furry",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "yiff" }
)
public class FurryCommand extends E621Abstract {

    public FurryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "sex";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}
