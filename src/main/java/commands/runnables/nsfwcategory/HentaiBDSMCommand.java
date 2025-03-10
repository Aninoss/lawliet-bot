package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "hbdsm",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaibdsm" }
)
public class HentaiBDSMCommand extends Rule34HentaiAbstract {

    public HentaiBDSMCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "bdsm";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "shemale", "gay", "futa", "futanari", "trap", "otoko_no_ko"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}