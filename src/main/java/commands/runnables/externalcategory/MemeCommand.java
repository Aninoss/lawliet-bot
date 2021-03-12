package commands.runnables.externalcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RedditAbstract;

@CommandProperties(
        trigger = "meme",
        emoji = "\uD83D\uDDBC",
        executableWithoutArgs = true,
        aliases = { "memes" }
)
public class MemeCommand extends RedditAbstract {

    public MemeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getSubreddit() {
        return "memes";
    }

}
