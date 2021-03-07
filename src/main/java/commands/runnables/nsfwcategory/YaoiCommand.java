package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.GelbooruAbstract;

@CommandProperties(
        trigger = "yaoi",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class YaoiCommand extends GelbooruAbstract implements OnTrackerRequestListener {

    public YaoiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated yaoi -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}