package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RedditNSFWAbstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_REAL_LIFE;

@CommandProperties(
        trigger = "rlbdsm",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "bdsm", "realllifebdsm" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_REAL_LIFE
)
public class RealLifeBDSMCommand extends RedditNSFWAbstract {

    public RealLifeBDSMCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "bdsm";
    }

}