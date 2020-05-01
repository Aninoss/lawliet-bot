package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.RealbooruAbstract;

@CommandProperties(
        trigger = "rlass",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patronRequired = true,
        withLoadingBar = true,
        aliases = {"ass"}
)
public class RealLifeAssCommand extends RealbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "animated ass -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}