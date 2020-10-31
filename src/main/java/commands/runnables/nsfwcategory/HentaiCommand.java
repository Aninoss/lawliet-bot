package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "hentai",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class HentaiCommand extends GelbooruAbstract implements OnTrackerRequestListener {

    public HentaiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated -yaoi -yuri -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}