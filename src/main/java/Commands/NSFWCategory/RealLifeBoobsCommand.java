package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.RealbooruAbstract;

@CommandProperties(
        trigger = "rlboobs",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        withLoadingBar = true,
        aliases = {"boobs"}
)
public class RealLifeBoobsCommand extends RealbooruAbstract {

    @Override
    protected String getSearchKey() {
        return "boobs -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}