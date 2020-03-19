package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "trap",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class TrapCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated_gif trap";
    }

    @Override
    protected boolean isGifOnly() {
        return true;
    }

}