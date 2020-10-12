package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RedditAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "meme",
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executableWithoutArgs = true
)
public class MemeCommand extends RedditAbstract {

    public MemeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getSubreddit() { return "memes"; }

}
