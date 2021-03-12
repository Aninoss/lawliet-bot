package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.GelbooruAbstract;

@CommandProperties(
        trigger = "hentai",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false
)
public class HentaiCommand extends GelbooruAbstract implements OnAlertListener {

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