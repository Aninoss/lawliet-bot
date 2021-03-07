package commands.runnables.externalcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RedditAbstract;

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
