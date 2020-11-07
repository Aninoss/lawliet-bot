package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.internet.HttpResponse;
import core.internet.InternetCache;
import core.utils.StringUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "pokemon",
    withLoadingBar = true,
    emoji = "\uD83C\uDDF5",
    exlusiveUsers = { 272037078919938058L, 397209883793162240L, 558626732308168765L },
    executableWithoutArgs = false
)
public class PokemonCommand extends Command {

    public PokemonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Pokemon pokemon = fetchPokemon(followedString.toLowerCase());
        if (pokemon == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        event.getChannel().sendMessage(getEmbed(pokemon)).get();
        return true;
    }

    private Pokemon fetchPokemon(String searchKey) throws ExecutionException, InterruptedException {
        String urlFormat = "https://www.bisafans.de/pokedex/name.php?name=%s";
        if (StringUtil.stringIsInt(searchKey)) {
            if (searchKey.length() > 3)
                return null;

            StringBuilder searchKeyBuilder = new StringBuilder(searchKey);
            while (searchKeyBuilder.length() < 3)
                searchKeyBuilder.insert(0, "0");
            searchKey = searchKeyBuilder.toString();

            urlFormat = "https://www.bisafans.de/pokedex/%s.php";
        }

        HttpResponse response = InternetCache.getData(String.format(urlFormat, searchKey), 60 * 60).get();
        if (response.getCode() != 200 || response.getContent().isEmpty())
            return null;
        String content = response.getContent().get();

        String title = StringUtil.extractGroups(content, "<section class=\"well\"><h1>", "</h1>")[0];
        if (!title.contains(" "))
            return null;
        String id = title.split(" ")[0].substring(1);
        String name = title.substring(id.length() + 2);
        String thumbnail = String.format("https://media.bisafans.de/34320abe7/pokemon/artwork/%s.png", id);
        String url = String.format("https://www.bisafans.de/pokedex/%s.php", id);
        return new Pokemon(id, name, thumbnail, url);
    }

    private EmbedBuilder getEmbed(Pokemon pokemon) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(getString("title", pokemon.id, pokemon.name))
                .setDescription(getString("desc", pokemon.url))
                .setUrl(pokemon.url)
                .setThumbnail(pokemon.thumbnail);
    }

    private static class Pokemon {

        private final String id;
        private final String name;
        private final String thumbnail;
        private final String url;

        public Pokemon(String id, String name, String thumbnail, String url) {
            this.id = id;
            this.name = name;
            this.thumbnail = thumbnail;
            this.url = url;
        }

    }

}