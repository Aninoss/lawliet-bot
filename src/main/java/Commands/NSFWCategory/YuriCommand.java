package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "yuri",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class YuriCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}