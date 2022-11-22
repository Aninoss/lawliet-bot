package commands.runnables.nsfwcategory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

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
public class HentaiAssCommand extends GelbooruAbstract {

    public HentaiAssCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "ass solo";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        HashSet<String> filters = new HashSet<>(super.getAdditionalFilters());
        filters.addAll(Set.of("1boy", "vaginal", "dildo", "2boys", "sex", "oral", "anal", "fellatio",
                "deep_penetration", "machinery", "machine", "electrostimulation", "anilingus", "double_anal",
                "double_penetration", "toy", "facefuck", "censored", "pregnant", "cigarette", "riding", "strap-on",
                "large_insertion", "vaginal_object_insertion", "futa", "futanari", "penis"));
        return filters;
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}