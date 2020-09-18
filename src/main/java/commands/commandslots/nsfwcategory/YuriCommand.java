package commands.commandslots.nsfwcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnTrackerRequestListener;
import commands.commandslots.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yuri",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class YuriCommand extends GelbooruAbstract implements OnTrackerRequestListener {

    public YuriCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}