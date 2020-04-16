package Commands.CasinoCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;
import Commands.CasinoAbstract;
import Constants.LetterEmojis;
import Constants.LogStatus;
import Constants.Settings;
import Core.*;
import Core.Internet.Internet;
import Core.Tools.StringTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quiz",
        emoji = "❔",
        thumbnail = "http://icons.iconarchive.com/icons/paomedia/small-n-flat/128/sign-question-icon.png",
        withLoadingBar = true,
        executable = true
)
public class QuizCommand extends CasinoAbstract implements OnReactionAddListener {

    final static Logger LOGGER = LoggerFactory.getLogger(QuizCommand.class);

    private String log;
    private LogStatus logStatus;
    private int difficulty;
    private String question;
    private String[] answers;
    private int correctAnswer;
    private int answerSelected;
    private final int COUNTER = 15;
    String url;

    public QuizCommand() {
        super();
        url = "https://opentdb.com/api.php?amount=1";
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {

            if (!allowBet) {
                logStatus = LogStatus.WARNING;
                log = TextManager.getString(getLocale(), TextManager.GENERAL, "nobet");
            }

            String dataString, diffString;
            JSONObject data;
            try {
                dataString = Internet.getData(url).get().getContent().get();
                data = new JSONObject(dataString).getJSONArray("results").getJSONObject(0);
                diffString = data.getString("difficulty");
            } catch (Throwable e) {
                DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(event.getMessageAuthor().getId()).changeValues(0, coinsInput);
                throw e;
            }

            switch (diffString) {
                case "easy":
                    difficulty = 0;
                    break;

                case "medium":
                    difficulty = 1;
                    break;

                case "hard":
                    difficulty = 2;
                    break;
            }

            question = StringTools.decryptString(data.getString("question"));

            ArrayList<String> orderedAnswers = new ArrayList<>();
            orderedAnswers.add(StringTools.decryptString(data.getString("correct_answer")));

            JSONArray answersJSON = data.getJSONArray("incorrect_answers");
            for(int i=0; i<answersJSON.length(); i++) {
                orderedAnswers.add(StringTools.decryptString(answersJSON.getString(i)));
            }

            answers = new String[orderedAnswers.size()];
            Random r = new Random();
            correctAnswer = -1;
            for(int i=0; i<answers.length; i++) {
                int select = r.nextInt(orderedAnswers.size());
                answers[i] = orderedAnswers.get(select);
                if (select == 0 && correctAnswer == -1) correctAnswer = i;
                orderedAnswers.remove(select);
            }

            compareKey = "quiz_" + answers.length + "_" + difficulty;
            winMultiplicator = answers.length * (difficulty+1) / 8.0;

            message = event.getChannel().sendMessage(getEmbed()).get();
            Thread t = new CustomThread(this::countdown, "quiz_countdown", 1);

            for(int i=0; i<answers.length; i++) message.addReaction(LetterEmojis.LETTERS[i]);

            return true;
        }
        return false;
    }

    private void countdown() {
        Thread.currentThread().setName("activity_countdown");
        try {
            Thread.sleep(COUNTER * 1000);
            if (active) {
                onAnswerSelected(-1);
            }
        } catch (InterruptedException | IOException | SQLException | ExecutionException e) {
            LOGGER.error("Exception on countdown", e);
        }
    }

    private void onAnswerSelected(int selected) throws IOException, SQLException, ExecutionException {
        if (selected == correctAnswer) {
            onWin();
            logStatus = LogStatus.WIN;
            log = getString("correct");
        } else {
            onLose();
            logStatus = LogStatus.LOSE;
            if (selected == -1) log = getString("timeup");
            else log = getString("wrong");
        }

        answerSelected = selected;
        message.edit(getEmbed());

        Thread t = new CustomThread(() -> {
            try {
                Thread.sleep(Settings.TIME_OUT_TIME);
            } catch (InterruptedException e) {
                //Ignore
            }
            try {
                deleteReactionMessage();
            } catch (InterruptedException e) {
                LOGGER.error("Could not remove message", e);
            }
        }, "quiz_countdown", 1);
        t.start();
    }

    private EmbedBuilder getEmbed() throws IOException {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .addField(getString("question"), question,false)
                .addField(getString("answers"), getAnswersString(),false);

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_footer"));

        String label = "tutorial";
        if (active) label = "tutorial_start";

        eb.addField(Settings.EMPTY_EMOJI, getString(label, server.getDisplayName(player), StringTools.numToString(getLocale(), coinsInput), String.valueOf(COUNTER)), false);

        eb = EmbedFactory.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getAnswersString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<answers.length; i++) {
            if (!active && correctAnswer == i) {
                sb.append("✅");
            } else if (!active && answerSelected == i) {
                sb.append("❌");
            } else
                sb.append(LetterEmojis.LETTERS[i]);
            sb.append(" | ").append(answers[i]).append("\n");
        }

        return sb.toString();
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji()) {
            for(int i=0; i<answers.length; i++) {
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(LetterEmojis.LETTERS[i])) {
                    onAnswerSelected(i);
                    break;
                }
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
