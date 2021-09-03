package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rldick",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "dick", "cock", "penis", "rlcock", "rlpenis" }
)
public class RealLifeDick extends RealbooruAbstract {

    public RealLifeDick(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "penis male_only";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("barry_allen", "pussy", "tits", "female", "oral", "2boys", "penetration", "celebrity", "panty_shot", "anal", "trap", "dildo", "transgender", "onahole", "shemale", "fleshlight", "fellatio", "self_suck", "milking_machine", "penis_lick", "deep_penetration", "outercourse", "fakes"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}