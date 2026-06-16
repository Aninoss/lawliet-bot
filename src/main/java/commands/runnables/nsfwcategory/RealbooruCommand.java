package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_SEARCH;

@CommandProperties(
        trigger = "realb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "rbooru", "realbooru", "rlbooru" },
        subCategory = NSFW_SUBCATEGORY_SEARCH,
        obsolete = true
)
public class RealbooruCommand extends PornSearchAbstract {

    public RealbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "realbooru.com";
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

}
