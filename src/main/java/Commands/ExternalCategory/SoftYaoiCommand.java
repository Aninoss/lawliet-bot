package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.SafebooruAbstract;

@CommandProperties(
        trigger = "softyaoi",
        emoji = "\uD83D\uDC6C",
        withLoadingBar = true,
        executable = true,
        aliases = {"safeyaoi"}
)
public class SoftYaoiCommand extends SafebooruAbstract {

    @Override
    protected String getSearchKey() {
        return "yaoi";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}