package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "r34p",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "rule34p", "34p", "r34paheal", "rule34paheal", "34paheal" }
)
public class Rule34PahealCommand extends PornSearchAbstract {

    public Rule34PahealCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "rule34.paheal.net";
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

    @Override
    public int getMaxTags() {
        return 3;
    }

}
