package commands.commandrunnables.externalcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.RedditAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "meme",
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executable = true
)
public class MemeCommand extends RedditAbstract {

    public MemeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getSubreddit() { return "memes"; }

}
