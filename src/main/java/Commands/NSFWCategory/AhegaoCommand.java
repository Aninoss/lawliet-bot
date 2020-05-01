package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "ahegao",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patronRequired = true,
        withLoadingBar = true
)
public class AhegaoCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated ahegao -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}