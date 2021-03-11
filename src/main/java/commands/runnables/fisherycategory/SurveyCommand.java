package commands.runnables.fisherycategory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.FisheryInterface;
import constants.*;
import core.*;
import core.utils.*;
import javafx.util.Pair;
import mysql.modules.survey.*;
import mysql.modules.tracker.TrackerSlot;
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
public class SurveyCommand extends Command implements FisheryInterface, OnStaticReactionAddListener, OnTrackerRequestListener {

    private static final String BELL_EMOJI = "🔔";

    public SurveyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws IOException {
        sendMessages(event.getChannel(), event.getMember(), false);
        return true;
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) throws IOException {
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), event.getChannel(), Permission.MESSAGE_MANAGE)) {
            return;
        }

        event.getReaction().removeReaction(event.getUser()).queue();

        removeUserReactions(message, JDAEmojiUtil.reactionEmoteAsMention(event.getReactionEmote()));
        String emoji = JDAEmojiUtil.reactionEmoteAsMention(event.getReactionEmote());
        for (byte i = 0; i < 2; i++) {
            int type = 0;
            if (emoji.equals(Emojis.LETTERS[i])) type = 1;
            if (emoji.equals(Emojis.RED_LETTERS[i])) type = 2;
            if (emoji.equals(BELL_EMOJI)) type = 3;

            if (type > 0) {
                SurveyBean surveyBean = DBSurvey.getInstance().getCurrentSurvey();

                if (message.getTimeCreated().toInstant().isAfter(surveyBean.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant())) {
                    if (!registerVote(event, surveyBean, type, i)) return;
                    EmbedBuilder eb = getVoteStatusEmbed(event, surveyBean);
                    JDAUtil.sendPrivateMessage(event.getMember(), eb.build()).queue();
                }
                break;
            }
        }
    }

    private EmbedBuilder getVoteStatusEmbed(GuildMessageReactionAddEvent event, SurveyBean surveyBean) throws IOException {
        SurveyQuestion surveyQuestion = surveyBean.getSurveyQuestionAndAnswers(getLocale());
        String[] voteStrings = new String[2];

        voteStrings[0] = "• " + surveyQuestion.getAnswers()[surveyBean.getFirstVotes().get(event.getUserIdLong()).getVote()];

        List<SurveySecondVote> surveySecondVotes = surveyBean.getSurveySecondVotesForUserId(event.getUserIdLong());

        if (surveySecondVotes.size() == 0) {
            voteStrings[1] = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            voteStrings[1] = "";
        }

        for (SurveySecondVote surveySecondVote : surveySecondVotes) {
            voteStrings[1] += "• " + surveyQuestion.getAnswers()[surveySecondVote.getVote()] + " (" + StringUtil.escapeMarkdown(ShardManager.getInstance().getGuildName(surveySecondVote.getGuildId()).orElse(String.valueOf(surveySecondVote.getGuildId()))) + ")\n";
        }

        return EmbedFactory.getEmbedDefault(this, getString("vote_description") + "\n" + Emojis.EMPTY_EMOJI)
                .addField(surveyQuestion.getQuestion(), voteStrings[0], false)
                .addField(getString("majority"), voteStrings[1], false)
                .addField(Emojis.EMPTY_EMOJI, getString("vote_notification", StringUtil.getOnOffForBoolean(getLocale(), surveyBean.hasNotificationUserId(event.getUserIdLong()))), false);
    }

    private void removeUserReactions(Message message, String addedEmoji) {
        for (MessageReaction reaction : message.getReactions()) {
            String emoji = JDAEmojiUtil.reactionEmoteAsMention(reaction.getReactionEmote());
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

    private boolean registerVote(GuildMessageReactionAddEvent event, SurveyBean surveyBean, int type, byte i) {
        switch (type) {
            case 1:
                surveyBean.getFirstVotes().put(event.getUserIdLong(), new SurveyFirstVote(event.getUserIdLong(), i, getLocale()));
                return true;

            case 2:
                if (surveyBean.getFirstVotes().containsKey(event.getUserIdLong())) {
                    surveyBean.getSecondVotes().put(
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
                if (surveyBean.getFirstVotes().containsKey(event.getUserIdLong())) {
                    surveyBean.toggleNotificationUserId(event.getUserIdLong());
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

    private Message sendMessages(TextChannel channel, Member member, boolean tracker) throws IOException {
        SurveyBean currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        SurveyBean lastSurvey = DBSurvey.getInstance().retrieve(currentSurvey.getSurveyId() - 1);

        //Results Message
        channel.sendMessage(getResultsEmbed(lastSurvey, member).build()).complete();

        //Survey Message
        EmbedBuilder eb = getSurveyEmbed(currentSurvey, tracker);
        if (!tracker) {
            EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());
        }
        Message message = channel.sendMessage(eb.build()).complete();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (i == 0) {
                    message.addReaction(Emojis.LETTERS[j]).complete();
                } else {
                    message.addReaction(Emojis.RED_LETTERS[j]).complete();
                }
            }
        }
        message.addReaction(BELL_EMOJI).complete();

        return message;
    }

    private EmbedBuilder getResultsEmbed(SurveyBean lastSurvey, Member member) throws IOException {
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

    private EmbedBuilder getSurveyEmbed(SurveyBean surveyBean, boolean tracker) throws IOException {
        SurveyQuestion surveyQuestion = surveyBean.getSurveyQuestionAndAnswers(getLocale());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("sdescription", BELL_EMOJI), getString("title") + Emojis.EMPTY_EMOJI)
                .setFooter("");

        StringBuilder personalString = new StringBuilder();
        StringBuilder majorityString = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            personalString.append(Emojis.LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
            majorityString.append(Emojis.RED_LETTERS[i]).append(" | ").append(surveyQuestion.getAnswers()[i]).append("\n");
        }
        eb.addField(surveyQuestion.getQuestion(), personalString.toString(), false);
        eb.addField(getString("majority"), majorityString.toString(), false);

        Instant after = TimeUtil.localDateToInstant(surveyBean.getNextDate());
        if (!tracker) {
            EmbedUtil.addLog(eb, LogStatus.TIME, getString("nextdate", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), after, false)));
        }
        EmbedUtil.addRemainingTime(eb, after);

        return eb;
    }

    @Override
    public String titleStartIndicator() {
        return getCommandProperties().emoji();
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        SurveyBean currentSurvey = DBSurvey.getInstance().getCurrentSurvey();
        if (slot.getArgs().isPresent() && currentSurvey.getSurveyId() <= Integer.parseInt(slot.getArgs().get())) {
            return TrackerResult.CONTINUE;
        }

        TextChannel channel = slot.getTextChannel().get();
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION)) {
            return TrackerResult.CONTINUE;
        }

        channel.deleteMessageById(slot.getMessageId().get()).complete();

        slot.setMessageId(sendMessages(channel, null, true).getIdLong());
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
