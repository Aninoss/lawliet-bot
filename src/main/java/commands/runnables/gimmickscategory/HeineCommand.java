package commands.runnables.gimmickscategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "heine",
        emoji = "\u270D\uFE0F️️",
        executableWithoutArgs = true,
        exclusiveUsers = { 509790627781672960L, 272037078919938058L }
)
public class HeineCommand extends Command {

    private static final String[] gifs = {
            "https://c.tenor.com/6xPOYZ-EzqkAAAAC/heine-heine-wittgenstein.gif",
            "https://c.tenor.com/DeRY8v8jqkEAAAAC/sachiikobayashi-haine-the-royal-tutor.gif",
            "https://c.tenor.com/-SamdlEYigsAAAAC/oushitsu-kyoushi-heine-walk.gif",
            "https://c.tenor.com/42eTrl0R1Y8AAAAC/anime-oushitsu.gif",
            "https://c.tenor.com/aWUW2DndXwMAAAAC/anime-royal-tutor.gif",
            "https://c.tenor.com/VObLfHfTdssAAAAC/royal-tutor-dance-heine.gif",
            "https://c.tenor.com/z9HQzeTr-8YAAAAC/cute-anime.gif"
    };

    public HeineCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        String gifUrl = gifs[RandomPicker.pick(getTrigger(), 0L, gifs.length).get()];
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle("Heine Gif")
                .setImage(gifUrl);
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
