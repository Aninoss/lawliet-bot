package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "ahegao",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        withLoadingBar = true
)
public class AhegaoCommand extends GelbooruAbstract {

    public AhegaoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated ahegao -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}