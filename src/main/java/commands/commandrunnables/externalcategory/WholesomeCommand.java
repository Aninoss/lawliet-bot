package commands.commandrunnables.externalcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.RedditAbstract;

import java.util.Locale;

@CommandProperties(
    trigger = "wholesome",
    emoji = "\uD83D\uDC96",
    executable = true
)
public class WholesomeCommand extends RedditAbstract {

    public WholesomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getSubreddit() { return "wholesomememes"; }

}
