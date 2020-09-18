package commands.commandrunnables.casinocategory;

import commands.commandlisteners.CommandProperties;
import constants.Permission;

import java.util.Locale;

@CommandProperties(
        trigger = "animequiz",
        emoji = "❔",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        withLoadingBar = true,
        deleteOnTimeOut = true,
        executable = true
)
public class AnimeQuizCommand extends QuizCommand {

    public AnimeQuizCommand(Locale locale, String prefix) {
        super(locale, prefix);
        url = "https://opentdb.com/api.php?amount=1&category=31";
        allowBet = false;
    }

}