package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "ahegao",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true
)
public class AhegaoCommand extends Rule34HentaiAbstract {

    public AhegaoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "ahe_gao -3d";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("yaoi", "shemale", "gay", "futa", "futanari", "trap", "otoko_no_ko", "3d",
                "blender_(medium)", "pixel_art"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}