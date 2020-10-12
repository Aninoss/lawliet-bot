package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;
import commands.runnables.SafebooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "softyuri",
        emoji = "\uD83D\uDC6D",
        withLoadingBar = true,
        executableWithoutArgs = true,
        aliases = {"safeyuri", "sfwyuri", "shoujoai", "shoujo-ai"}
)
public class SoftYuriCommand extends SafebooruAbstract {

    public SoftYuriCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}