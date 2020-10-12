package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "rlass",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        withLoadingBar = true,
        aliases = {"ass", "r1ass"}
)
public class RealLifeAssCommand extends RealbooruAbstract {

    public RealLifeAssCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated ass -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}