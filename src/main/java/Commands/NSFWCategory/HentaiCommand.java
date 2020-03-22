package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.GelbooruAbstract;

@CommandProperties(
        trigger = "hentai",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class HentaiCommand extends GelbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}