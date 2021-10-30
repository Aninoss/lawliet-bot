package commands.runnables.nsfwcategory;

import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "tartagliansfw",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        exclusiveUsers = { 509790627781672960L, 272037078919938058L },
        aliases = { "tartaglia" }
)
public class TartagliaNSFWCommand extends Rule34HentaiAbstract {

    public TartagliaNSFWCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "tartaglia_(genshin_impact)";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        return super.getAdditionalFilters();
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}