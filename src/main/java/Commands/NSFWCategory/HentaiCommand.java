package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "hentai",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class HentaiCommand extends GelbooruAbstract implements OnTrackerRequestListener {

    @Override
    protected String getSearchKey() {
        return "animated -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}