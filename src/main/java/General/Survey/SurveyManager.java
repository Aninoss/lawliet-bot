package General.Survey;

import General.DiscordApiCollection;
import General.FileManager;
import MySQL.Modules.Survey.DBSurvey;
import MySQL.Modules.Survey.SurveyBean;
import MySQL.Modules.Survey.SurveySecondVote;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SurveyManager {

    public static int getCurrentFirstVotesNumber() throws SQLException, ExecutionException {
        return DBSurvey.getInstance().getCurrentSurvey().getFirstVotes().size();
    }

    public static SurveyQuestion getSurveyQuestionAndAnswers(int surveyId, Locale locale) throws IOException {
        List<String> surveyList = FileManager.readInList(new File("recourses/survey_" + locale.getDisplayName() + ".txt"));
        while(surveyId >= surveyList.size()) surveyId -= surveyList.size();
        String[] parts = surveyList.get(surveyId).split("\\|"); //0 = Question, 1 = 1st Answer, 2 = 2nd Answer
        return new SurveyQuestion(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }

    public static long getFirstVoteNumbers(SurveyBean surveyBean) {
        return surveyBean.getFirstVotes().size();
    }

    public static long getFirstVoteNumbers(SurveyBean surveyBean, byte vote) {
        return surveyBean.getFirstVotes().values().stream().filter(secondVote -> secondVote.getVote() == vote).count();
    }

    public static List<SurveySecondVote> getSurveySecondVotesForUserId(SurveyBean surveyBean, long userId) {
        return surveyBean.getSecondVotes().values().stream()
                .filter(surveySecondVote -> surveySecondVote.getUserId() == userId && DiscordApiCollection.getInstance().getServerById(surveySecondVote.getServerId()).isPresent())
                .collect(Collectors.toList());
    }

    public static byte getWon(SurveyBean surveyBean) {
        long votesA = getFirstVoteNumbers(surveyBean, (byte)0);
        long votesTotal = getFirstVoteNumbers(surveyBean);
        long votesB = votesTotal - votesA;

        if (votesA > votesB) return 0;
        if (votesA < votesB) return 1;
        return 2;
    }

}
