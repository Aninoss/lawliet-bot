package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;
import Constants.PatreonMode;

@CommandProperties(
        trigger = "ahegao",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patronMode = PatreonMode.USER_LOCK,
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