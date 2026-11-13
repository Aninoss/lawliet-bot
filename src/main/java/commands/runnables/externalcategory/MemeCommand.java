package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;

import java.util.Locale;

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
