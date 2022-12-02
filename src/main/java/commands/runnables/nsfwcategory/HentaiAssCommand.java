package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.DanbooruAbstract;

@CommandProperties(
        trigger = "hass",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaiass", "hbutt", "hentaibutt" }
)
public class HentaiAssCommand extends DanbooruAbstract {

    public HentaiAssCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "ass solo -sex";
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