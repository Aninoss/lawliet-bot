package commands.commandslots.externalcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandslots.SafebooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "softyaoi",
        emoji = "\uD83D\uDC6C",
        withLoadingBar = true,
        executable = true,
        aliases = {"safeyaoi", "sfwyaoi", "shounenai", "shounen-ai"}
)
public class SoftYaoiCommand extends SafebooruAbstract {

    public SoftYaoiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "yaoi";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}