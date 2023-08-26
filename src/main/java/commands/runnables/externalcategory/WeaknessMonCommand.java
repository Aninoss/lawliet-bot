package commands.runnables.externalcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.TextManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "weaknessmon",
        emoji = "\uD83C\uDDF5",
        exclusiveUsers = { 397209883793162240L, 381156056660967426L, 272037078919938058L },
        executableWithoutArgs = false
)
public class WeaknessMonCommand extends WeaknessTypeCommand {

    private PokemonCommand.Pokemon pokemon;

    public WeaknessMonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getContent(CommandEvent event, String args, List<Integer> types) {
        String[] typesString = TextManager.getString(getLocale(), Category.EXTERNAL, "weaknesstype_types").split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typesString.length; i++) {
            if (types.contains(i)) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(typesString[i]);
            }
        }

        return getString("desc", pokemon.getTitle().split("–")[0], sb.toString());
    }

    @Override
    protected List<Integer> retrieveTypes(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        pokemon = PokemonCommand.fetchPokemon(args);
        if (pokemon == null) {
            return new ArrayList<>();
        }

        return pokemon.getTypes();
    }

}