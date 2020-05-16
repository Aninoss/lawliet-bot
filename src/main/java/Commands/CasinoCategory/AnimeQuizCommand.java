package Commands.CasinoCategory;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "animequiz",
        emoji = "❔",
        withLoadingBar = true,
        deleteOnTimeOut = true,
        executable = true
)
public class AnimeQuizCommand extends QuizCommand {

    public AnimeQuizCommand() {
        super();
        url = "https://opentdb.com/api.php?amount=1&category=31";
        allowBet = false;
    }

}