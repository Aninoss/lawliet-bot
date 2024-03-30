package commands.runnables.utilitycategory;

import commands.listeners.CommandProperties;

import java.util.Locale;

@CommandProperties(
        trigger = "multivote",
        emoji = "\uD83D\uDDF3",
        executableWithoutArgs = false,
        aliases = {"multipoll"}
)
public class MultiVoteCommand extends VoteCommand {

    public MultiVoteCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

}
