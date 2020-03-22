package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "futa",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class FutaCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated futa";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}