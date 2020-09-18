package commands.commandrunnables.nsfwcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnTrackerRequestListener;
import commands.commandrunnables.RealbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "rlporn",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class RealLifePornCommand extends RealbooruAbstract implements OnTrackerRequestListener {

    public RealLifePornCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}