package commands.runnables.casinocategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.runnables.CasinoAbstract;
import constants.*;
import core.CustomThread;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.internet.HttpRequest;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quiz",
        emoji = "❔",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        withLoadingBar = true,
        deleteOnTimeOut = true,
        executableWithoutArgs = true
)
public class QuizCommand extends CasinoAbstract implements OnReactionAddListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(QuizCommand.class);

    private String log;
    private LogStatus logStatus;
    private int difficulty;
    private String question;
    private String[] answers;
    private int correctAnswer;
    private int answerSelected;
    private final int COUNTER = 10;
    String url;

    public QuizCommand(Locale locale, String prefix) {
        super(locale, prefix);
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
                dataString = HttpRequest.getData(url).get().getContent().get();
                data = new JSONObject(dataString).getJSONArray("results").getJSONObject(0);
                diffString = data.getString("difficulty");
            } catch (Throwable e) {
                DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId()).changeValues(0, coinsInput);
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

            question = StringUtil.decryptString(data.getString("question"));

            ArrayList<String> orderedAnswers = new ArrayList<>();
            orderedAnswers.add(StringUtil.decryptString(data.getString("correct_answer")));

            JSONArray answersJSON = data.getJSONArray("incorrect_answers");
            for(int i=0; i<answersJSON.length(); i++) {
                orderedAnswers.add(StringUtil.decryptString(answersJSON.getString(i)));
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
            new CustomThread(this::countdown, "quiz_countdown", 1).start();

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
        } catch (InterruptedException | IOException | ExecutionException e) {
            LOGGER.error("Exception on countdown", e);
        }
    }

    private void onAnswerSelected(int selected) throws IOException, ExecutionException, InterruptedException {
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
                removeReactionListenerWithMessage();
            } catch (InterruptedException e) {
                LOGGER.error("Could not remove message", e);
            }
        }, "quiz_countdown", 1);
        t.start();
    }

    private EmbedBuilder getEmbed() {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .addField(getString("question"), question,false)
                .addField(getString("answers"), getAnswersString(),false);

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        String label = "tutorial";
        if (active) label = "tutorial_start";

        CustomEmoji countdown = DiscordApiCollection.getInstance().getHomeEmojiById(729371766119727124L);
        eb.addField(Emojis.EMPTY_EMOJI, getString(label, server.getDisplayName(player), StringUtil.numToString(coinsInput), countdown.getMentionTag()), false);

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
