package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "kchan",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "konac", "konachan", "konnac", "konnachan" }
)
public class KonachanCommand extends PornSearchAbstract {

    public KonachanCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "konachan.com";
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

    @Override
    public int getMaxTags() {
        return 6;
    }

}
