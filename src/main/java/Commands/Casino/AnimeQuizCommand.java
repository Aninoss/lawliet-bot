package Commands.Casino;

import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import Constants.LetterEmojis;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Settings;
import General.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;


public class AnimeQuizCommand extends QuizCommand {
    public AnimeQuizCommand() {
        super();
        trigger = "animequiz";
        url = "https://opentdb.com/api.php?amount=1&category=31";
        allowBet = false;
    }
}