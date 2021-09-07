package commands.runnables.externalcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.Category;
import core.TextManager;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    protected String getContent(GuildMessageReceivedEvent event, String args, List<Integer> types) {
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
    protected List<Integer> retrieveTypes(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        pokemon = PokemonCommand.fetchPokemon(args);
        if (pokemon == null) {
            return new ArrayList<>();
        }

        return pokemon.getTypes();
    }

}