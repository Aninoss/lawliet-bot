package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "rlporn",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "porn", "reallifeporn", "sex", "rlsex", "reallifesex" }
)
public class RealLifePornCommand extends RealbooruAbstract {

    public RealLifePornCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "sex";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("lesbian", "gay", "shemale", "trap", "transgender", "otoko_no_ko", "censored"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}