package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "rlfemabs",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "reallifefemaleabs" }
)
public class RealLifeFemaleAbsCommand extends RealbooruAbstract {

    public RealLifeFemaleAbsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "muscular_female abs";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("lesbian", "gay", "shemale", "trap", "transgender", "otoko_no_ko", "censored", "strapon",
                "vaginal", "anal"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}