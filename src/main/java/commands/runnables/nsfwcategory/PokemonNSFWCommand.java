package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.E621Abstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_TEMPLATES_RULE_34;

@CommandProperties(
        trigger = "r34pokemon",
        executableWithoutArgs = true,
        emoji = "🔞",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "nsfwpokemon" },
        subCategory = NSFW_SUBCATEGORY_TEMPLATES_RULE_34
)
public class PokemonNSFWCommand extends E621Abstract {

    public PokemonNSFWCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "pokemon sex";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}
