package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

@CommandProperties(
        trigger = "bara",
        executableWithoutArgs = true,
        emoji = "🔞",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false
)
public class BaraCommand extends DanbooruAbstract {

    public BaraCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "bara";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("furry"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}