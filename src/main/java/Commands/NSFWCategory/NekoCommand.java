package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "neko",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class NekoCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "cat_girl -futa";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}