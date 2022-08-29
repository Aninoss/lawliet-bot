package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rlpussy",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "pussy", "reallifepussy", "vagina", "rlvagina", "reallifevagina" }
)
public class RealLifePussy extends RealbooruAbstract {

    public RealLifePussy(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "pussy solo";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("1boy", "vaginal", "dildo", "2boys", "sex", "oral", "penetration", "celebrity", "anal",
                "fellatio", "deep_penetration", "fakes", "machine", "electrostimulation", "anilingus", "double_anal",
                "double_penetration", "toy", "face_fuck", "censored", "ass", "breasts", "pregnant", "nipples",
                "cigarette", "cartoon", "riding", "spider-man_(series)", "strap-on", "large_insertion",
                "vaginal_insertion", "gaping"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}