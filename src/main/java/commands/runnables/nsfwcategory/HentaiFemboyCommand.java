package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_HENTAI;

@CommandProperties(
        trigger = "hfemboy",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaifemboy", "trap" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_HENTAI
)
public class HentaiFemboyCommand extends Rule34HentaiAbstract {

    public HentaiFemboyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "femboy";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}