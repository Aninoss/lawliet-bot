package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RedditAbstract;

import java.util.Locale;

@CommandProperties(
    trigger = "wholesome",
    emoji = "\uD83D\uDC96",
    executableWithoutArgs = true
)
public class WholesomeCommand extends RedditAbstract {

    public WholesomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getSubreddit() {
        return "wholesomememes";
    }

}
