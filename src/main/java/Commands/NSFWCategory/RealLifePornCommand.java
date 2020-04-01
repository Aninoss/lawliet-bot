package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.RealbooruAbstract;

@CommandProperties(
        trigger = "rlporn",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true
)
public class RealLifePornCommand extends RealbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}