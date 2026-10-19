package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.Rule34Abstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_RULE_34;

@CommandProperties(
        trigger = "r34roblox",
        executableWithoutArgs = true,
        emoji = "🔞",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "roblox" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_RULE_34
)
public class RobloxCommand extends Rule34Abstract {

    public RobloxCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "roblox sex";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "yuri", "shemale", "lesbian", "gay", "futa", "futanari", "trap", "otoko_no_ko", "pixel_art"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}
