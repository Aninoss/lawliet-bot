package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "hfemabs",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaifemaleabs" }
)
public class HentaiFemaleAbsCommand extends DanbooruAbstract {

    public HentaiFemaleAbsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "muscular_female abs";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "shemale", "gay", "futa", "futanari", "trap", "otoko_no_ko", "pixel_art"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}