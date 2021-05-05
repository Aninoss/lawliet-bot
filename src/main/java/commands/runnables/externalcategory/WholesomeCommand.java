package commands.runnables.externalcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;

@CommandProperties(
        trigger = "wholesome",
        emoji = "\uD83D\uDC96",
        executableWithoutArgs = true
)
public class WholesomeCommand extends RedditCommand {

    public WholesomeCommand(Locale locale, String prefix) {
        super(locale, prefix, "wholesome");
    }

}
