package Commands.Casino;

import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import Constants.LetterEmojis;
import Constants.LogStatus;
import Constants.Settings;
import General.*;
import General.Internet.Internet;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;


public class QuizCommand extends Casino implements onRecievedListener, onReactionAddListener {
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
        botPermissions = 0;
        userPermissions = 0;
        trigger = "quiz";
        emoji = "❔";
        thumbnail = "http://icons.iconarchive.com/icons/paomedia/small-n-flat/128/sign-question-icon.png";
        withLoadingBar = true;
        url = "https://opentdb.com/api.php?amount=1";
        deleteOnTimeOut = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {

            String dataString = Internet.getData(url);
            JSONObject data = new JSONObject(dataString).getJSONArray("results").getJSONObject(0);
            String diffString = data.getString("difficulty");

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

            question = Shortcuts.decryptString(data.getString("question"));

            ArrayList<String> orderedAnswers = new ArrayList<>();
            orderedAnswers.add(Shortcuts.decryptString(data.getString("correct_answer")));

            JSONArray answersJSON = data.getJSONArray("incorrect_answers");
            for(int i=0; i<answersJSON.length(); i++) {
                orderedAnswers.add(Shortcuts.decryptString(answersJSON.getString(i)));
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
            new Thread(this::countdown).start();

            for(int i=0; i<answers.length; i++) message.addReaction(LetterEmojis.LETTERS[i]);

            return true;
        }
        return false;
    }

    private void countdown() {
        try {
            Thread.sleep(COUNTER * 1000);
            if (active) {
                onAnswerSelected(-1);
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, channel);
        }
    }

    private void onAnswerSelected(int selected) throws Throwable {
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

        new Thread(() -> {
            try {
                Thread.sleep(Settings.TIME_OUT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                deleteReactionMessage();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }).start();
    }

    private EmbedBuilder getEmbed() throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .addField(getString("question"), question,false)
                .addField(getString("answers"), getAnswersString(),false);

        if (coinsInput != 0) eb.setFooter(TextManager.getString(locale, TextManager.COMMANDS, "casino_footer"));

        String label = "tutorial";
        if (active) label = "tutorial_start";

        eb.addField(Tools.getEmptyCharacter(), getString(label, server.getDisplayName(player), Tools.numToString(locale, coinsInput), String.valueOf(COUNTER)), false);

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
    public void onReactionTimeOut(Message message) throws Throwable {}
}
