package commands.runnables.casinocategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.components.ActionRows;
import core.internet.HttpRequest;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quiz",
        emoji = "❔",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        deleteOnTimeOut = true,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "trivia" }
)
public class QuizCommand extends CasinoAbstract {

    private int difficulty;
    private String question;
    private String[] answers;
    private int correctAnswer;
    private int answerSelected;
    private final String url;

    public QuizCommand(Locale locale, String prefix) {
        this(locale, prefix, "https://opentdb.com/api.php?amount=1");
    }

    public QuizCommand(Locale locale, String prefix, String url) {
        super(locale, prefix, false, true);
        this.url = url;
    }

    @Override
    public boolean onGameStart(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        String dataString, diffString;
        JSONObject data;
        deferReply();
        dataString = HttpRequest.get(url).get().getBody();
        data = new JSONObject(dataString).getJSONArray("results").getJSONObject(0);
        diffString = data.getString("difficulty");

        switch (diffString) {
            case "easy" -> difficulty = 0;
            case "medium" -> difficulty = 1;
            case "hard" -> difficulty = 2;
        }

        question = StringUtil.decryptString(data.getString("question"));
        if (question == null || question.trim().isEmpty()) {
            drawMessageNew(EmbedFactory.getApiDownEmbed(this, "opentdb.com"))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        ArrayList<String> orderedAnswers = new ArrayList<>();
        orderedAnswers.add(StringUtil.decryptString(data.getString("correct_answer")));

        JSONArray answersJSON = data.getJSONArray("incorrect_answers");
        for (int i = 0; i < answersJSON.length(); i++) {
            orderedAnswers.add(StringUtil.decryptString(answersJSON.getString(i)));
        }

        answers = new String[orderedAnswers.size()];
        Random r = new Random();
        correctAnswer = -1;
        for (int i = 0; i < answers.length; i++) {
            int select = r.nextInt(orderedAnswers.size());
            answers[i] = orderedAnswers.get(select);
            if (select == 0 && correctAnswer == -1) correctAnswer = i;
            orderedAnswers.remove(select);
        }

        setCompareKey("quiz_" + answers.length + "_" + difficulty);
        schedule(Duration.ofSeconds(10), () -> onTimeUp(event.getMember()));
        return true;
    }

    @Override
    public boolean onButtonCasino(ButtonInteractionEvent event) throws Throwable {
        int i = Integer.parseInt(event.getComponentId());
        onAnswerSelected(event.getMember(), i);
        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        if (question != null && answers != null) {
            eb.addField(getString("question"), question, false);
            setAnswersButtons();
        }

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        String label = "tutorial";
        if (getStatus() == Status.ACTIVE) {
            label = "tutorial_start";
        }

        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString(label, StringUtil.escapeMarkdown(playerName), StringUtil.numToString(coinsInput), Emojis.COUNTDOWN_10.getFormatted()), false);
        return eb;
    }

    private void setAnswersButtons() {
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < answers.length; i++) {
            ButtonStyle style;
            if (getStatus() != Status.ACTIVE && correctAnswer == i) {
                style = ButtonStyle.SUCCESS;
            } else if (getStatus() != Status.ACTIVE && answerSelected == i) {
                style = ButtonStyle.DANGER;
            } else {
                style = ButtonStyle.SECONDARY;
            }
            Button button = Button.of(style, String.valueOf(i), StringUtil.shortenString(answers[i], 80))
                    .withDisabled(getStatus() != Status.ACTIVE);
            buttons.add(button);
        }

        ArrayList<ActionRow> actionRows = new ArrayList<>(ActionRows.of(buttons));
        if (getStatus() != Status.ACTIVE) {
            Button button = Button.of(ButtonStyle.PRIMARY, BUTTON_ID_RETRY, TextManager.getString(getLocale(), Category.CASINO, "casino_retry"));
            actionRows.add(ActionRow.of(button));
        }
        setActionRows(actionRows);
    }

    private void onTimeUp(Member member) {
        if (getStatus() == Status.ACTIVE) {
            onAnswerSelected(member, -1);
            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
        }
    }

    private void onAnswerSelected(Member member, int selected) {
        if (selected == correctAnswer) {
            win(member, answers.length * (difficulty + 1) / 8.0);
            setLog(LogStatus.WIN, getString("correct"));
        } else {
            lose(member, true);
            setLog(LogStatus.LOSE, selected == -1 ? getString("timeup") : getString("wrong"));
        }

        answerSelected = selected;
    }

}
