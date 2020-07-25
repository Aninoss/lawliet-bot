package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import Commands.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yaoi",
        executable = true,
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