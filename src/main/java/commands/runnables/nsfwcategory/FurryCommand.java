package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.E621Abstract;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_HENTAI;

@CommandProperties(
        trigger = "furry",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "yiff" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_HENTAI
)
public class FurryCommand extends E621Abstract {

    public FurryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "sex";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}
