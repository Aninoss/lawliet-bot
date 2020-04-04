package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LetterEmojis;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Settings;
import General.*;
import General.Survey.Survey;
import General.Survey.SurveyResults;
import General.Survey.UserMajorityVoteData;
import General.Survey.UserVoteData;
import General.Tools.StringTools;
import General.Tools.TimeTools;
import General.Tracker.TrackerData;
import MySQL.DBSurvey;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "survey",
    botPermissions = Permission.MANAGE_MESSAGES,
    thumbnail = "http://icons.iconarchive.com/icons/iconarchive/blue-election/128/Election-Polling-Box-icon.png",
    emoji = "✅",
    executable = true
)
public class SurveyCommand extends Command implements onReactionAddStaticListener, onTrackerRequestListener {

    private static long lastAccess = 0;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Survey survey = DBSurvey.getCurrentSurvey();
        sendMessages(event.getServerTextChannel().get(), survey, false);
        return true;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getTrigger(), event.getServerTextChannel().get(), Permission.MANAGE_MESSAGES)) return;
        event.removeReaction().get();

        for(Reaction reaction: message.getReactions()) {
            boolean correctEmoji = false;
            for (int i = 0; i < 2; i++) {
                if (reaction.getEmoji().isUnicodeEmoji() && (reaction.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(LetterEmojis.LETTERS[i]) || reaction.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(LetterEmojis.RED_LETTERS[i]))) {
                    correctEmoji = true;
                    break;
                }
            }

            if (!correctEmoji) reaction.remove().get();
            else if (reaction.getCount() > 1) {
                for(User user: reaction.getUsers().get()) {
                    if (!user.isYourself()) reaction.removeUser(user).get();
                }
            }
        }

        if (event.getEmoji().isUnicodeEmoji()) {
            for (int i = 0; i < 2; i++) {
                int hit = 0;
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(LetterEmojis.LETTERS[i])) hit = 1;
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(LetterEmojis.RED_LETTERS[i])) hit = 2;

                if (hit > 0) {
                    Survey survey = DBSurvey.getCurrentSurvey();

                    if (message.getCreationTimestamp().isAfter(survey.getStart())) {
                        if (hit == 1) DBSurvey.updatePersonalVote(event.getUser(), i);
                        else {
                            if (!DBSurvey.updateMajorityVote(event.getServer().get(), event.getUser(), i)) {
                                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("vote_error"), TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"));
                                event.getUser().sendMessage(eb);
                                return;
                            }
                        }
                        List<String> surveyList = FileManager.readInList(new File("recourses/survey_" + getLocale().getDisplayName() + ".txt"));
                        int n = survey.getId();
                        while(n >= surveyList.size()) n -= surveyList.size();
                        String[] surveyData = surveyList.get(n).split("\\|");
                        UserVoteData votes = DBSurvey.getUserVotes(event.getUser());

                        String[] voteStrings = new String[2];

                        voteStrings[0] = "• " + surveyData[votes.getPersonalVote() + 1];

                        ArrayList<UserMajorityVoteData> userMajorityVoteDataList = votes.getMajorityVotes();
                        if (userMajorityVoteDataList.size() == 0) voteStrings[1] = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                        else voteStrings[1] = "";

                        for (UserMajorityVoteData userMajorityVoteData: userMajorityVoteDataList) {
                            voteStrings[1] += "• " + surveyData[userMajorityVoteData.getVote() + 1] + " (" + userMajorityVoteData.getServer().getName() + ")\n";
                        }

                        EmbedBuilder eb = EmbedFactory.getCommandEmbedSuccess(this, getString("vote_description") + "\n" + Settings.EMPTY_EMOJI)
                                .addField(surveyData[0], voteStrings[0])
                                .addField(getString("majority"), voteStrings[1]);

                        event.getUser().sendMessage(eb);
                    }
                    break;
                }
            }
        }
    }

    private Message sendMessages(ServerTextChannel channel, Survey survey, boolean tracker) throws InterruptedException, IOException, SQLException, ExecutionException {
        while(lastAccess != 0 && System.currentTimeMillis() <= lastAccess + 1000 * 60) {
            Thread.sleep(1000);
        }

        lastAccess = System.currentTimeMillis();

        //Results Message
        channel.sendMessage(getResultsEmbed());

        //Survey Message
        EmbedBuilder eb = getSurveyEmbed(survey);
        if (!tracker) EmbedFactory.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        Message message = channel.sendMessage(eb).get();

        for(int i=0; i<2; i++) {
            for(int j=0; j<2; j++) {
                if (i == 0) message.addReaction(LetterEmojis.LETTERS[j]).get();
                else message.addReaction(LetterEmojis.RED_LETTERS[j]).get();
            }
        }

        lastAccess = 0;

        return message;
    }

    private EmbedBuilder getResultsEmbed() throws SQLException, IOException {
        SurveyResults surveyResults = DBSurvey.getResults();
        String[] surveyData = surveyResults.getQuestionAndAnswers(getLocale());

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, "", getString("results_title"));
        eb.addField(getString("results_question"), surveyData[0], false);

        StringBuilder answerString = new StringBuilder();
        for(int i=0; i<2; i++) {
            answerString.append(LetterEmojis.LETTERS[i]).append(" | ").append(surveyData[i+1]).append("\n");
        }
        eb.addField(getString("results_answers"), answerString.toString(), false);

        StringBuilder resultString = new StringBuilder();
        for(int i=0; i<2; i++) {
            resultString.append(
                    getString("results_template",
                            LetterEmojis.LETTERS[i],
                            StringTools.getBar(surveyResults.getUserVoteRelative(i), 12),
                            String.valueOf(surveyResults.getUserVote(i)),
                            String.valueOf((int) Math.round(surveyResults.getUserVoteRelative(i)*100))
                    )
            ).append("\n");
        }
        eb.addField(getString("results_results", surveyResults.getTotalUserVotes() != 1, String.valueOf(surveyResults.getTotalUserVotes())), resultString.toString(), false);
        eb.addField(Settings.EMPTY_EMOJI, getString("results_won", surveyResults.getWinner(), surveyData[1], surveyData[2]).toUpperCase());

        return eb;
    }

    private EmbedBuilder getSurveyEmbed(Survey survey) throws IOException {
        String[] surveyData = getSurveyData(survey.getId(), getLocale());
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("sdescription"), getString("title") + Settings.EMPTY_EMOJI);

        StringBuilder personalString = new StringBuilder();
        StringBuilder majorityString = new StringBuilder();
        for(int i=0; i<2; i++) {
            personalString.append(LetterEmojis.LETTERS[i]).append(" | ").append(surveyData[i+1]).append("\n");
            majorityString.append(LetterEmojis.RED_LETTERS[i]).append(" | ").append(surveyData[i+1]).append("\n");
        }
        eb.addField(surveyData[0], personalString.toString(), false);
        eb.addField(getString("majority"), majorityString.toString(), false);

        return eb;
    }

    public static String[] getSurveyData(int surveyId, Locale locale) throws IOException {
        List<String> surveyList = FileManager.readInList(new File("recourses/survey_" + locale.getDisplayName() + ".txt"));
        while(surveyId >= surveyList.size()) surveyId -= surveyList.size();
        return surveyList.get(surveyId).split("\\|"); //0 = Question, 1 = 1st Answer, 2 = 2nd Answer
    }

    @Override
    public String getTitleStartIndicator() {
        return getEmoji();
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        while(trackerData.getArg() != null && DBSurvey.getCurrentSurvey().getId() <= Integer.parseInt(trackerData.getArg())) {
            Thread.sleep(60 * 1000);
        }

        ServerTextChannel channel = trackerData.getChannel().get();
        if (!PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getTrigger(), channel, Permission.ADD_REACTIONS)) {
            trackerData.setSaveChanges(false);
            return trackerData;
        }
        trackerData.deletePreviousMessage();
        Survey survey = DBSurvey.getCurrentSurvey();
        trackerData.setMessageDelete(sendMessages(channel, survey, true));
        Instant nextInstant = trackerData.getInstant();
        do {
            nextInstant = TimeTools.setInstantToNextDay(nextInstant);
        } while(!TimeTools.instantHasWeekday(nextInstant, Calendar.MONDAY) && !TimeTools.instantHasWeekday(nextInstant, Calendar.THURSDAY));

        trackerData.setInstant(nextInstant.plusSeconds(5 * 60));
        trackerData.setArg(String.valueOf(survey.getId()));
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
