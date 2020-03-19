package Commands.CasinoCategory;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "animequiz",
        emoji = "❔",
        thumbnail = "http://icons.iconarchive.com/icons/paomedia/small-n-flat/128/sign-question-icon.png",
        withLoadingBar = true,
        executable = true
)
public class AnimeQuizCommand extends QuizCommand {

    public AnimeQuizCommand() {
        super();
        url = "https://opentdb.com/api.php?amount=1&category=31";
        allowBet = false;
    }

}