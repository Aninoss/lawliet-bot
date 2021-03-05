package commands.runnables.casinocategory;

import commands.listeners.CommandProperties;
import constants.PermissionDeprecated;

import java.util.Locale;

@CommandProperties(
        trigger = "animequiz",
        emoji = "❔",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        withLoadingBar = true,
        deleteOnTimeOut = true,
        executableWithoutArgs = true
)
public class AnimeQuizCommand extends QuizCommand {

    public AnimeQuizCommand(Locale locale, String prefix) {
        super(locale, prefix);
        url = "https://opentdb.com/api.php?amount=1&category=31";
        allowBet = false;
    }

}