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
        return "animated_gif -yaoi -yuri";
    }

    @Override
    protected boolean isGifOnly() {
        return true;
    }

}