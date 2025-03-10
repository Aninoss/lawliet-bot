package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "yuri",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false
)
public class YuriCommand extends Rule34HentaiAbstract {

    public YuriCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "yuri";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("futa", "futanari"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}