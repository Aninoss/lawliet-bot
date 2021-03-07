package commands.runnables.externalcategory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.internet.HttpResponse;
import core.internet.InternetCache;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
    trigger = "pokemon",
    withLoadingBar = true,
    emoji = "\uD83C\uDDF5",
    exclusiveUsers = { 397209883793162240L, 381156056660967426L },
    executableWithoutArgs = false
)
public class PokemonCommand extends Command {

    public PokemonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Pokemon pokemon = fetchPokemon(followedString.toLowerCase());
        if (pokemon == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getNoResultsString(getLocale(), followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        event.getChannel().sendMessage(getEmbed(pokemon)).get();
        return true;
    }

    private Pokemon fetchPokemon(String searchKey) throws ExecutionException, InterruptedException {
        HttpResponse response = InternetCache.getData("https://www.pokewiki.de/" + searchKey, 60 * 60).get();
        if (response.getCode() != 200 || response.getContent().isEmpty())
            return null;
        String content = response.getContent().get();

        String title = StringUtil.extractGroups(content, "<meta property=\"og:title\" content=\"", "\"/>")[0];
        if (!title.contains(" "))
            return null;
        String desc = StringUtil.extractGroups(content, "<meta property=\"og:description\" content=\"", "\"/>")[0];

        String thumbnail = "";
        if (content.contains("<meta property=\"og:image\" content=\""))
            thumbnail = StringUtil.extractGroups(content, "<meta property=\"og:image\" content=\"", "\"/>")[0];

        String url = StringUtil.extractGroups(content, "<meta property=\"og:url\" content=\"", "\"/>")[0];
        return new Pokemon(title, desc, thumbnail, url);
    }

    private EmbedBuilder getEmbed(Pokemon pokemon) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(pokemon.title)
                .setDescription(pokemon.description)
                .setUrl(pokemon.url)
                .setThumbnail(pokemon.thumbnail);
    }

    private static class Pokemon {

        private final String title;
        private final String description;
        private final String thumbnail;
        private final String url;

        public Pokemon(String title, String description, String thumbnail, String url) {
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
            this.url = url;
        }

    }

}