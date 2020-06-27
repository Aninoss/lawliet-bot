package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnTrackerRequestListener;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "yaoi",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class YaoiCommand extends GelbooruAbstract implements OnTrackerRequestListener {

    @Override
    protected String getSearchKey() {
        return "animated yaoi";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}