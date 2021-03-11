package commands.runnables.casinocategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;

@CommandProperties(
        trigger = "animequiz",
        emoji = "❔",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        withLoadingBar = true,
        deleteOnTimeOut = true,
        executableWithoutArgs = true
)
public class AnimeQuizCommand extends QuizCommand {

    public AnimeQuizCommand(Locale locale, String prefix) {
        super(locale, prefix, false, "https://opentdb.com/api.php?amount=1&category=31");
    }

}