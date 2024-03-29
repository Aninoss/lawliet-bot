package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "r34",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "rule34", "34" }
)
public class Rule34Command extends PornSearchAbstract {

    public Rule34Command(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "rule34.xxx";
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

}
