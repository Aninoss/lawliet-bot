package commands.runnables.externalcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.SafebooruAbstract;

@CommandProperties(
        trigger = "softyuri",
        emoji = "\uD83D\uDC6D",
        executableWithoutArgs = true,
        requiresEmbeds = false,
        aliases = { "safeyuri", "sfwyuri", "shoujoai", "shoujo-ai" }
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