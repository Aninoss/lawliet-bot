package commands.runnables.externalcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;

@CommandProperties(
        trigger = "meme",
        emoji = "\uD83D\uDDBC",
        executableWithoutArgs = true,
        aliases = { "memes" }
)
public class MemeCommand extends RedditCommand {

    public MemeCommand(Locale locale, String prefix) {
        super(locale, prefix, "memes");
    }

}
