package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;
import Commands.Rule34Abstract;

@CommandProperties(
        trigger = "furry",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class FurryCommand extends Rule34Abstract {

    @Override
    protected String getSearchKey() {
        return "animated furry";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}