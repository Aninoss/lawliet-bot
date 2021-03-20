package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rlboobs",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 3 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "boobs", "r1boobs" }
)
public class RealLifeBoobsCommand extends RealbooruAbstract {

    public RealLifeBoobsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "boobs -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}