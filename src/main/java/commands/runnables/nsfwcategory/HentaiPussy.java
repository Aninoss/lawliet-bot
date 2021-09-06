package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

@CommandProperties(
        trigger = "hpussy",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "hvagina" }
)
public class HentaiPussy extends DanbooruAbstract {

    public HentaiPussy(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "pussy solo -sex -breasts -anal -ass";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("1boy", "vaginal", "dildo", "2boys", "sex", "oral", "penetration", "anal", "fellatio",
                "deep_penetration", "machine", "electrostimulation", "anilingus", "double_anal", "double_penetration",
                "toy", "face_fuck", "censored", "ass", "breasts", "pregnant", "nipples", "cigarette", "cartoon",
                "riding", "strap-on", "large_insertion", "vaginal_insertion", "gaping", "pee", "peeing", "piss",
                "pissing", "futa", "penis"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}