package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "futa",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
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