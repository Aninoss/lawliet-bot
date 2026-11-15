package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;

import java.util.Locale;

@CommandProperties(
        trigger = "wholesome",
        emoji = "\uD83D\uDC96",
        executableWithoutArgs = true,
        maxCalculationTimeSec = 5 * 60
)
public class WholesomeCommand extends RedditCommand {

    public WholesomeCommand(Locale locale, String prefix) {
        super(locale, prefix, "wholesome");
    }

}
