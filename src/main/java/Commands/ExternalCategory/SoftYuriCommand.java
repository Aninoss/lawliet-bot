package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.SafebooruAbstract;

@CommandProperties(
        trigger = "softyuri",
        emoji = "\uD83D\uDC6D",
        withLoadingBar = true,
        executable = true,
        aliases = {"safeyuri"}
)
public class SoftYuriCommand extends SafebooruAbstract {

    @Override
    protected String getSearchKey() {
        return "yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}