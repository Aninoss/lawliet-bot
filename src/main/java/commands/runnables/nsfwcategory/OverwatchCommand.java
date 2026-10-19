package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.Rule34Abstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_RULE_34;

@CommandProperties(
        trigger = "r34overwatch",
        executableWithoutArgs = true,
        emoji = "🔞",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "overwatch" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_RULE_34
)
public class OverwatchCommand extends Rule34Abstract {

    public OverwatchCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "overwatch sex 3d";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "trap", "otoko_no_ko"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}
