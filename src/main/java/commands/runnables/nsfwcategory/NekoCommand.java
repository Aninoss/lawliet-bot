package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "neko",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class NekoCommand extends GelbooruAbstract {

    public NekoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "cat_girl -futa";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}