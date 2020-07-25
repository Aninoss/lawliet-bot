package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import Commands.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "hentai",
        executable = true,
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
        return "animated -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}