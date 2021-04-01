package commands.runnables.externalcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
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
        return getString("desc", pokemon.getTitle().split("–")[0]);
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