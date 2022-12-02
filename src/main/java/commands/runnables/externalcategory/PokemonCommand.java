package commands.runnables.externalcategory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.internet.HttpCache;
import core.internet.HttpResponse;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "pokemon",
        emoji = "\uD83C\uDDF5",
        exclusiveUsers = { 397209883793162240L, 381156056660967426L, 272037078919938058L },
        executableWithoutArgs = false
)
public class PokemonCommand extends Command {

    public PokemonCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        Pokemon pokemon = fetchPokemon(args);
        if (pokemon == null) {
            drawMessageNew(EmbedFactory.getNoResultsEmbed(this, args))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        drawMessageNew(getEmbed(pokemon)).exceptionally(ExceptionLogger.get());
        return true;
    }

    public static Pokemon fetchPokemon(String searchKey) throws ExecutionException, InterruptedException {
        HttpResponse response = HttpCache.get("https://www.pokewiki.de/" + URLEncoder.encode(searchKey.replace(" ", "_"), StandardCharsets.UTF_8), Duration.ofDays(1)).get();
        if (response.getCode() != 200 || response.getBody() == null) {
            return null;
        }
        String content = response.getBody();

        String title = StringUtil.extractGroups(content, "<meta property=\"og:title\" content=\"", "\"/>")[0];
        if (!title.contains(" ")) {
            return null;
        }
        String desc = StringUtil.extractGroups(content, "<meta property=\"og:description\" content=\"", "\"/>")[0];

        String thumbnail = null;
        if (content.contains("<meta property=\"og:image\" content=\"")) {
            thumbnail = StringUtil.extractGroups(content, "<meta property=\"og:image\" content=\"", "\"/>")[0];
        }

        ArrayList<Integer> types = new ArrayList<>();
        String groupStart = "<td style=\"background:#ffffff; width:40%\"><a href=\"/Typen\" title=\"Typen\">";
        String groupEnd = "<td style=\"background:#ffffff; width:40%\"><a href=\"/Pok%C3%A9mon-Liste\" title=\"Pokémon-Liste\">National-Dex</a>";

        if (content.contains(groupStart) && content.contains(groupEnd)) {
            String groupsBase = StringUtil.extractGroups(content, groupStart, groupEnd)[0];
            if (groupsBase.contains("<span style=\"font-size:x-small;\">")) {
                int i = groupsBase.indexOf("<span style=\"font-size:x-small;\">");
                groupsBase = groupsBase.substring(0, i);
            }
            String[] lines = groupsBase.split("<br />");
            for (String line : lines) {
                boolean found = false;
                String[] groups = StringUtil.extractGroups(line, "><span class=\"typ-icon\" title=\"", "\">");
                for (String group : groups) {
                    String[] typesString = TextManager.getString(Language.DE.getLocale(), Category.EXTERNAL, "weaknesstype_types").split("\n");
                    for (int i = 0; i < typesString.length; i++) {
                        if (group.equals(typesString[i]) && !types.contains(i) && types.size() < 2) {
                            types.add(i);
                            found = true;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        String url = StringUtil.extractGroups(content, "<meta property=\"og:url\" content=\"", "\"/>")[0];
        return new Pokemon(title, desc, thumbnail, url, types);
    }

    private EmbedBuilder getEmbed(Pokemon pokemon) {
        return EmbedFactory.getEmbedDefault()
                .setTitle(pokemon.title, pokemon.url)
                .setDescription(pokemon.description)
                .setThumbnail(pokemon.thumbnail);
    }

    public static class Pokemon {

        private final String title;
        private final String description;
        private final String thumbnail;
        private final String url;
        private final List<Integer> types;

        public Pokemon(String title, String description, String thumbnail, String url, List<Integer> types) {
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
            this.url = url;
            this.types = types;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public String getUrl() {
            return url;
        }

        public List<Integer> getTypes() {
            return types;
        }

    }

}