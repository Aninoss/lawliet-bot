package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_HENTAI;

@CommandProperties(
        trigger = "genshinyaoi",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "genshinimpactyaoi" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_HENTAI
)
public class GenshinYaoiCommand extends DanbooruAbstract {

    public GenshinYaoiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "genshin_impact yaoi rating:e";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("shemale", "trap", "otoko_no_ko"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}