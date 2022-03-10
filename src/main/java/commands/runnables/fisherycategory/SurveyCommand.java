package commands.runnables.fisherycategory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnStaticButtonListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import modules.schedulers.AlertResponse;
import mysql.modules.survey.*;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "survey",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = Emojis.CHECKMARK,
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class SurveyCommand extends Command implements FisheryInterface, OnStaticButtonListener, OnAlertListener {

    private static final String BUTTON_ID_VOTE_FIRST_A = "vote_1_0";
    private static final String BUTTON_ID_VOTE_FIRST_B = "vote_1_1";
    private static final String BUTTON_ID_VOTE_SECOND_A = "vote_2_0";
    private static final String BUTTON_ID_VOTE_SECOND_B = "vote_2_1";

    public SurveyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws IOException, InterruptedException {
        SurveyEmbeds surveyEmbeds = generateSurveyEmbeds(event.getMember());

        setActionRows(surveyEmbeds.actionRows);
        setAdditionalEmbeds(surveyEmbeds.newEmbed.build());
        drawMessageNew(surveyEmbeds.resultEmbed)
                .thenAccept(this::registerStaticReactionMessage)
                .exceptionally(ExceptionLogger.get());
        return true;
    }

    @Override
    public void onStaticButton(ButtonClickEvent event) throws Throwable {
        SurveyData surveyData = DBSurvey.getInstance().getCurrentSurvey();
        if (event.getMessage().getTimeCreated().toInstant().isAfter(surveyData.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant())) {
            String[] parts = event.getComponentId().split("_");
            byte type = Byte.parseByte(parts[1]);
            byte vote = Byte.parseByte(parts[2]);

            if (registerVote(event, surveyData, type, vote)) {
                EmbedBuilder eb = getVoteStatusEmbed(event.getMember(), surveyData);
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            }
        }
    }

    private EmbedBuilder getVoteStatusEmbed(Member member, SurveyData surveyData) throws IOException {
        SurveyQuestion surveyQuestion = surveyData.getSurveyQuestionAndAnswers(getLocale());
        String[] voteStrings = new String[2];

        voteStrings[0] = "• " + surveyQuestion.getAnswers()[surveyData.getFirstVotes().get(member.getIdLong()).getVote()];

        SurveySecondVote surveySecondVote = surveyData.getSecondVotes().get(new Pair<>(member.getGuild().getIdLong(), member.getIdLong()));
        if (surveySecondVote == null) {
            voteStrings[1] = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            voteStrings[1] = "• " + surveyQuestion.getAnswers()[surveySecondVote.getVote()] + " (" + StringUtil.escapeMarkdown(ShardManager.getGuildName(surveySecondVote.getGuildId()).orElse(String.valueOf(surveySecondVote.getGuildId()))) + ")\n";
        }

        return EmbedFactory.getEmbedDefault(this, getString("vote_description") + "\n" + Emojis.ZERO_WIDTH_SPACE)
                .addField(surveyQuestion.getQuestion(), voteStrings[0], false)
                .addField(getString("majority"), StringUtil.shortenStringLine(voteStrings[1], 1024), false);
    }

    private boolean registerVote(ButtonClickEvent event, SurveyData surveyData, int type, byte i) {
        Member member = event.getMember();
        switch (type) {
            case 1:
                surveyData.getFirstVotes().put(member.getIdLong(), new SurveyFirstVote(member.getIdLong(), i, getLocale()));
                return true;

            case 2:
                if (surveyData.getFirstVotes().containsKey(member.getIdLong())) {
                    surveyData.getSecondVotes().put(
                            new Pair<>(member.getGuild().getIdLong(), member.getIdLong()),
                            new SurveySecondVote(member.getGuild().getIdLong(), member.getIdLong(), i)
                    );
                    return true;
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("vote_error"), TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"));
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                    return false;
                }

            case 3:
                if (surveyData.getFirstVotes().containsKey(member.getIdLong())) {
                    surveyData.toggleNotificationUserId(member.getIdLong());
                    return true;
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("vote_error"), TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"));
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                    return false;
                }

            default:
                return false;
        }
    }

    private SurveyEmbeds generateSurveyEmbeds(Member member) throws IOException {
        SurveyData currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        SurveyData lastSurvey = DBSurvey.getInstance().retrieve(currentSurvey.getSurveyId() - 1);

        EmbedBuilder newEmbed = generateNewEmbed(currentSurvey, member == null);
        if (member != null) {
            EmbedUtil.addTrackerNoteLog(getLocale(), member, newEmbed, getPrefix(), getTrigger());
        }

        String[] answers = currentSurvey.getSurveyQuestionAndAnswers(getLocale()).getAnswers();
        ActionRow[] actionRows = new ActionRow[] {
                ActionRow.of(
                        Button.of(ButtonStyle.PRIMARY, BUTTON_ID_VOTE_FIRST_A, getString("button_first", answers[0])),
                        Button.of(ButtonStyle.PRIMARY, BUTTON_ID_VOTE_FIRST_B, getString("button_first", answers[1]))
                ),
                ActionRow.of(
                        Button.of(ButtonStyle.SUCCESS, BUTTON_ID_VOTE_SECOND_A, getString("button_second", answers[0])),
                        Button.of(ButtonStyle.SUCCESS, BUTTON_ID_VOTE_SECOND_B, getString("button_second", answers[1]))
                )
        };

        return new SurveyEmbeds(
                generateResultEmbed(lastSurvey, member),
                newEmbed,
                actionRows
        );
    }

    private EmbedBuilder generateResultEmbed(SurveyData lastSurvey, Member member) throws IOException {
        SurveyQuestion surveyQuestion = lastSurvey.getSurveyQuestionAndAnswers(getLocale());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, "", getString("results_title"));
        eb.addField(getString("results_question"), surveyQuestion.getQuestion(), false);

        StringBuilder answerString = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            answerString.append(Emojis.LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
        }
        eb.addField(getString("results_answers"), answerString.toString(), false);

        long firstVotesTotal = lastSurvey.getFirstVoteNumber();
        long[] firstVotes = new long[2];
        for (byte i = 0; i < 2; i++) firstVotes[i] = lastSurvey.getFirstVoteNumbers(i);
        double[] firstVotesRelative = new double[2];
        for (byte i = 0; i < 2; i++) firstVotesRelative[i] = firstVotes[i] / (double) firstVotesTotal;

        StringBuilder resultString = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            resultString.append(
                    getString(
                            "results_template",
                            Emojis.LETTERS[i],
                            StringUtil.getBar(firstVotesRelative[i], 12),
                            StringUtil.numToString(firstVotes[i]),
                            StringUtil.numToString(Math.round(firstVotesRelative[i] * 100))
                    )
            ).append("\n");
        }

        eb.addField(getString("results_results", firstVotesTotal != 1, StringUtil.numToString(firstVotesTotal)), resultString.toString(), false);
        eb.setTimestamp(TimeUtil.localDateToInstant(lastSurvey.getStartDate()));

        boolean individual = false;
        if (member != null && lastSurvey.getWon() != 2) {
            SurveySecondVote secondVote = lastSurvey.getSecondVotes().get(new Pair<>(member.getGuild().getIdLong(), member.getIdLong()));
            if (secondVote != null) {
                individual = true;
                boolean won = lastSurvey.getWon() == 2 || lastSurvey.getWon() == secondVote.getVote();
                EmbedUtil.addLog(eb, won ? LogStatus.WIN : LogStatus.LOSE, getString("results_status", won));
            }
        }

        if (!individual) {
            EmbedUtil.addLog(eb, null, getString("results_won", lastSurvey.getWon(), surveyQuestion.getAnswers()[0], surveyQuestion.getAnswers()[1]));
        }

        return eb;
    }

    private EmbedBuilder generateNewEmbed(SurveyData surveyData, boolean tracker) throws IOException {
        SurveyQuestion surveyQuestion = surveyData.getSurveyQuestionAndAnswers(getLocale());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("sdescription"), getString("title"))
                .setFooter("");

        StringBuilder personalString = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            personalString.append(Emojis.LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
        }
        eb.addField(surveyQuestion.getQuestion(), personalString.toString(), false);

        Instant after = TimeUtil.localDateToInstant(surveyData.getNextDate());
        eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("nextdate", TimeFormat.DATE_TIME_LONG.atInstant(after).toString()), false);

        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        SurveyData currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        if (slot.getArgs().isPresent() && currentSurvey.getSurveyId() <= Integer.parseInt(slot.getArgs().get())) {
            return AlertResponse.CONTINUE;
        }

        TextChannel channel = slot.getTextChannel().get();
        if (!PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
            return AlertResponse.CONTINUE;
        }

        slot.getMessageId().ifPresent(messageId -> channel.deleteMessageById(messageId).queue());

        SurveyEmbeds surveyEmbeds = generateSurveyEmbeds(null);
        slot.sendMessage(true, surveyEmbeds.resultEmbed.build()).get();
        long messageId = slot.sendMessage(false, surveyEmbeds.newEmbed.build(), surveyEmbeds.actionRows).get();
        registerStaticReactionMessage(slot.getTextChannel().get(), messageId);

        slot.setMessageId(messageId);
        slot.setNextRequest(getNextSurveyInstant(Instant.now()));
        slot.setArgs(String.valueOf(currentSurvey.getSurveyId()));

        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private Instant getNextSurveyInstant(Instant start) {
        do {
            start = TimeUtil.setInstantToNextDay(start);
        } while (!TimeUtil.instantHasWeekday(start, Calendar.MONDAY) && !TimeUtil.instantHasWeekday(start, Calendar.THURSDAY));
        return start;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

    private static class SurveyEmbeds {

        private final EmbedBuilder resultEmbed;
        private final EmbedBuilder newEmbed;
        private final ActionRow[] actionRows;

        public SurveyEmbeds(EmbedBuilder resultEmbed, EmbedBuilder newEmbed, ActionRow[] actionRows) {
            this.resultEmbed = resultEmbed;
            this.newEmbed = newEmbed;
            this.actionRows = actionRows;
        }

    }

}
