package commands.runnables.fisherycategory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.LogStatus;
import constants.TrackerResult;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.ShardManager;
import core.TextManager;
import core.utils.*;
import javafx.util.Pair;
import mysql.modules.survey.*;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@CommandProperties(
        trigger = "survey",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = Emojis.CHECKMARK,
        executableWithoutArgs = true
)
public class SurveyCommand extends Command implements FisheryInterface, OnStaticReactionAddListener, OnAlertListener {

    private static final String BELL_EMOJI = "🔔";

    public SurveyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws IOException, InterruptedException {
        sendMessages(event.getChannel(), event.getMember(), false, (i, eb) -> event.getChannel().sendMessage(eb).complete().getIdLong());
        return true;
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) throws IOException {
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), event.getChannel(), Permission.MESSAGE_MANAGE)) {
            return;
        }

        event.getReaction().removeReaction(event.getUser()).queue();

        removeUserReactions(message, EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()));
        String emoji = EmojiUtil.reactionEmoteAsMention(event.getReactionEmote());
        for (byte i = 0; i < 2; i++) {
            int type = 0;
            if (emoji.equals(Emojis.LETTERS[i])) type = 1;
            if (emoji.equals(Emojis.RED_LETTERS[i])) type = 2;
            if (emoji.equals(BELL_EMOJI)) type = 3;

            if (type > 0) {
                SurveyData surveyData = DBSurvey.getInstance().getCurrentSurvey();

                if (message.getTimeCreated().toInstant().isAfter(surveyData.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant())) {
                    if (!registerVote(event, surveyData, type, i)) return;
                    EmbedBuilder eb = getVoteStatusEmbed(event, surveyData);
                    JDAUtil.sendPrivateMessage(event.getMember(), eb.build()).queue();
                }
                break;
            }
        }
    }

    private EmbedBuilder getVoteStatusEmbed(GuildMessageReactionAddEvent event, SurveyData surveyData) throws IOException {
        SurveyQuestion surveyQuestion = surveyData.getSurveyQuestionAndAnswers(getLocale());
        String[] voteStrings = new String[2];

        voteStrings[0] = "• " + surveyQuestion.getAnswers()[surveyData.getFirstVotes().get(event.getUserIdLong()).getVote()];

        List<SurveySecondVote> surveySecondVotes = surveyData.getSurveySecondVotesForUserId(event.getUserIdLong());

        if (surveySecondVotes.size() == 0) {
            voteStrings[1] = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            voteStrings[1] = "";
        }

        for (SurveySecondVote surveySecondVote : surveySecondVotes) {
            voteStrings[1] += "• " + surveyQuestion.getAnswers()[surveySecondVote.getVote()] + " (" + StringUtil.escapeMarkdown(ShardManager.getInstance().getGuildName(surveySecondVote.getGuildId()).orElse(String.valueOf(surveySecondVote.getGuildId()))) + ")\n";
        }

        return EmbedFactory.getEmbedDefault(this, getString("vote_description") + "\n" + Emojis.ZERO_WIDTH_SPACE)
                .addField(surveyQuestion.getQuestion(), voteStrings[0], false)
                .addField(getString("majority"), StringUtil.shortenStringLine(voteStrings[1], 1024), false)
                .addField(Emojis.ZERO_WIDTH_SPACE, getString("vote_notification", StringUtil.getOnOffForBoolean(getLocale(), surveyData.hasNotificationUserId(event.getUserIdLong()))), false);
    }

    private void removeUserReactions(Message message, String addedEmoji) {
        for (MessageReaction reaction : message.getReactions()) {
            String emoji = EmojiUtil.reactionEmoteAsMention(reaction.getReactionEmote());
            boolean correctEmoji = false;
            for (int i = 0; i < 2; i++) {
                if (emoji.equals(Emojis.LETTERS[i]) ||
                        emoji.equals(Emojis.RED_LETTERS[i]) ||
                        emoji.equals(BELL_EMOJI)
                ) {
                    correctEmoji = true;
                    break;
                }
            }

            if (correctEmoji && reaction.getCount() > (addedEmoji.equals(emoji) ? 2 : 1)) {
                for (User user : reaction.retrieveUsers()) {
                    if (user.getIdLong() != ShardManager.getInstance().getSelfId()) {
                        reaction.removeReaction(user).queue();
                    }
                }
            }
        }
    }

    private boolean registerVote(GuildMessageReactionAddEvent event, SurveyData surveyData, int type, byte i) {
        switch (type) {
            case 1:
                surveyData.getFirstVotes().put(event.getUserIdLong(), new SurveyFirstVote(event.getUserIdLong(), i, getLocale()));
                return true;

            case 2:
                if (surveyData.getFirstVotes().containsKey(event.getUserIdLong())) {
                    surveyData.getSecondVotes().put(
                            new Pair<>(event.getGuild().getIdLong(), event.getUserIdLong()),
                            new SurveySecondVote(event.getGuild().getIdLong(), event.getUserIdLong(), i)
                    );
                    return true;
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("vote_error"), TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"));
                    JDAUtil.sendPrivateMessage(event.getMember(), eb.build()).queue();
                    return false;
                }

            case 3:
                if (surveyData.getFirstVotes().containsKey(event.getUserIdLong())) {
                    surveyData.toggleNotificationUserId(event.getUserIdLong());
                    return true;
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("vote_error"), TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"));
                    JDAUtil.sendPrivateMessage(event.getMember(), eb.build()).queue();
                    return false;
                }

            default:
                return false;
        }
    }

    private long sendMessages(TextChannel channel, Member member, boolean tracker, BiFunction<Integer, MessageEmbed, Long> messageFunction) throws IOException {
        SurveyData currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        SurveyData lastSurvey = DBSurvey.getInstance().retrieve(currentSurvey.getSurveyId() - 1);

        //Results Message
        messageFunction.apply(0, getResultsEmbed(lastSurvey, member).build());

        //Survey Message
        EmbedBuilder eb = getSurveyEmbed(currentSurvey, tracker);
        if (!tracker) {
            EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());
        }

        long messageId = messageFunction.apply(1, eb.build());
        registerStaticReactionMessage(channel, messageId);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (i == 0) {
                    channel.addReactionById(messageId, Emojis.LETTERS[j]).complete();
                } else {
                    channel.addReactionById(messageId, Emojis.RED_LETTERS[j]).complete();
                }
            }
        }
        channel.addReactionById(messageId, BELL_EMOJI).complete();

        return messageId;
    }

    private EmbedBuilder getResultsEmbed(SurveyData lastSurvey, Member member) throws IOException {
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

    private EmbedBuilder getSurveyEmbed(SurveyData surveyData, boolean tracker) throws IOException {
        SurveyQuestion surveyQuestion = surveyData.getSurveyQuestionAndAnswers(getLocale());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("sdescription", BELL_EMOJI), getString("title"))
                .setFooter("");

        StringBuilder personalString = new StringBuilder();
        StringBuilder majorityString = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            personalString.append(Emojis.LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
            majorityString.append(Emojis.RED_LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
        }
        eb.addField(surveyQuestion.getQuestion(), personalString.toString(), false);
        eb.addField(getString("majority"), majorityString.toString(), false);

        Instant after = TimeUtil.localDateToInstant(surveyData.getNextDate());
        if (!tracker) {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("nextdate", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), after, false)));
        }
        EmbedUtil.addRemainingTime(eb, after);

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerData slot) throws Throwable {
        SurveyData currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        if (slot.getArgs().isPresent() && currentSurvey.getSurveyId() <= Integer.parseInt(slot.getArgs().get())) {
            return TrackerResult.CONTINUE;
        }

        TextChannel channel = slot.getTextChannel().get();
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
            return TrackerResult.CONTINUE;
        }

        slot.getMessageId().ifPresent(messageId -> channel.deleteMessageById(messageId).queue());
        slot.setMessageId(sendMessages(channel, null, true, (i, eb) -> slot.sendMessage(i == 0, eb).get()));
        slot.setNextRequest(getNextSurveyInstant(Instant.now()));
        slot.setArgs(String.valueOf(currentSurvey.getSurveyId()));

        return TrackerResult.CONTINUE_AND_SAVE;
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

}
