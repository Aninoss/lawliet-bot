package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "hthighs",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "hentaithighs" }
)
public class HentaiThighsCommand extends DanbooruAbstract {

    public HentaiThighsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "thighs solo -sex";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("1boy", "vaginal", "dildo", "2boys", "sex", "oral", "penetration", "anal", "fellatio",
                "deep_penetration", "machine", "electrostimulation", "anilingus", "double_anal", "double_penetration",
                "toy", "face_fuck", "censored", "pregnant", "cigarette", "cartoon",
                "riding", "strap-on", "large_insertion", "vaginal_insertion", "gaping", "pee", "peeing", "piss",
                "pissing", "futa", "futanari", "penis"));
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