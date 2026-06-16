package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RedditNSFWAbstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_REAL_LIFE;

@CommandProperties(
        trigger = "rlthreesome",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "reallifthreesome" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_REAL_LIFE
)
public class RealLifeThreesomeCommand extends RedditNSFWAbstract {

    public RealLifeThreesomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "Threesome";
    }

}