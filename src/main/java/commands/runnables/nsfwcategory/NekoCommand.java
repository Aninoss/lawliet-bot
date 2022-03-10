package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

@CommandProperties(
        trigger = "neko",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "nekogirl", "nekogirls", "catgirl", "catgirls" }
)
public class NekoCommand extends DanbooruAbstract {

    public NekoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    /*@Override
    protected String getSearchKey() {
        return "cat_girl rating:e";
    } TODO: revert back if danbooru.com works again */

    @Override
    protected String getSearchKey() {
        return "catgirl";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "yuri", "shemale", "lesbian", "gay", "futa", "futanari", "trap", "otoko_no_ko"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}