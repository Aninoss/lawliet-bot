package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "yaoi",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class YaoiCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated_gif yaoi";
    }

    @Override
    protected boolean isGifOnly() {
        return true;
    }

}