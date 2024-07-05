package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rlass",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "ass", "reallifeass", "butt", "rlbutt", "reallifebutt" }
)
public class RealLifeAssCommand extends RealbooruAbstract {

    public RealLifeAssCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "ass";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("gay", "shemale", "trap", "transgender", "otoko_no_ko", "sex", "dildo",
                "orgasm", "1boy", "2boys", "penis", "penetration", "double_penetration", "fellatio", "handjob",
                "oral", "threesome", "anal", "vaginal", "vaginal_sex", "doggy_style", "squirt", "squirting",
                "vaginal_penetration", "fingering", "cum", "buttjob", "cum_in_pussy", "vaginal_insertion",
                "mia_khalifa", "reverse_cowgirl_position", "riding", "enema", "breasts"));
        return filters;
    }

    @Override
    public boolean mustBeExplicit() {
        return false;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}