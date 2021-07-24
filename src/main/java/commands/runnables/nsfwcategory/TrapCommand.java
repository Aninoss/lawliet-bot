package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "trap",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false
)
public class TrapCommand extends Rule34HentaiAbstract {

    public TrapCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated trap";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}