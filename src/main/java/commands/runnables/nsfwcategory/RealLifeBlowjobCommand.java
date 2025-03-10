package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rlblowjob",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "reallifeblowjob" }
)
public class RealLifeBlowjobCommand extends RealbooruAbstract {

    public RealLifeBlowjobCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "fellatio";
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
        return true;
    }

}