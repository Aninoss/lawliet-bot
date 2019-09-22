package General.Survey;

import General.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SurveyResults {
    private int[] userVotes;
    private int[] majorityVotes;
    private int surveyId;

    public SurveyResults(int[] userVotes, int[] majorityVotes, int surveyId) {
        this.userVotes = userVotes;
        this.majorityVotes = majorityVotes;
        this.surveyId = surveyId;
    }

    public int getUserVote(int i) {
        return userVotes[i];
    }

    public int getMajorityVote(int i) {
        return majorityVotes[i];
    }

    public double getUserVoteRelative(int i) {
        return (double) userVotes[i] / getTotalUserVotes();
    }

    public int getTotalUserVotes() {
        return userVotes[0] + userVotes[1];
    }

    public double getMajorityVoteRelative(int i) {
        return (double) majorityVotes[i] / getTotalMajorityVotes();
    }

    public int getTotalMajorityVotes() {
        return majorityVotes[0] + majorityVotes[1];
    }

    public String[] getQuestionAndAnswers(Locale locale) throws IOException {
        List<String> surveyList = FileManager.readInList(new File("recourses/survey_" + locale.getDisplayName() + ".txt"));
        return surveyList.get(surveyId).split("\\|");
    }

    public int getWinner() {
        if (userVotes[0] > userVotes[1]) return 0;
        if (userVotes[0] < userVotes[1]) return 1;
        return 2;
    }

    public int getSurveyId() {
        return surveyId;
    }
}