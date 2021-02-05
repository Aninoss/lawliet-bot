package mysql.modules.survey;

import core.CustomObservableList;
import core.CustomObservableMap;
import core.FileManager;
import core.ResourceHandler;
import javafx.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SurveyBean extends Observable {

    private final int surveyId;
    private final LocalDate startDate;
    private final CustomObservableMap<Long, SurveyFirstVote> firstVotes;
    private final CustomObservableMap<Pair<Long, Long>, SurveySecondVote> secondVotes; /* Pair: serverId, userId */
    private final CustomObservableList<Long> notificationUserIds;

    public SurveyBean(int surveyId, LocalDate startDate, @NonNull HashMap<Long, SurveyFirstVote> firstVotes,
                      @NonNull HashMap<Pair<Long, Long>, SurveySecondVote> secondVotes, @NonNull ArrayList<Long> notificationUserIds) {
        this.surveyId = surveyId;
        this.startDate = startDate;
        this.firstVotes = new CustomObservableMap<>(firstVotes);
        this.secondVotes = new CustomObservableMap<>(secondVotes);
        this.notificationUserIds = new CustomObservableList<>(notificationUserIds);
    }


    /* Getters */

    public int getSurveyId() { return surveyId; }

    public LocalDate getStartDate() { return startDate; }

    public CustomObservableMap<Long, SurveyFirstVote> getFirstVotes() {
        return firstVotes;
    }

    public CustomObservableMap<Pair<Long, Long>, SurveySecondVote> getSecondVotes() {
        return secondVotes;
    }

    public LocalDate getNextDate() {
        LocalDate localDate = getStartDate();
        do {
            localDate = localDate.plus(1, ChronoUnit.DAYS);
        } while(localDate.getDayOfWeek() != DayOfWeek.MONDAY && localDate.getDayOfWeek() != DayOfWeek.THURSDAY);

        return localDate;
    }

    public CustomObservableList<Long> getNotificationUserIds() {
        return notificationUserIds;
    }

    public boolean hasNotificationUserId(long userId) {
        return notificationUserIds.contains(userId);
    }

    public void toggleNotificationUserId(long userId) {
        if (hasNotificationUserId(userId)) {
            notificationUserIds.remove(userId);
        } else {
            notificationUserIds.add(userId);
        }
    }


    /* Tools */

    public SurveyQuestion getSurveyQuestionAndAnswers(Locale locale) throws IOException {
        List<String> surveyList = FileManager.readInList(ResourceHandler.getFileResource("data/resources/survey_" + locale.getDisplayName() + ".txt"));
        int serverIdTemp = surveyId;

        while(serverIdTemp >= surveyList.size()) serverIdTemp -= surveyList.size();
        String[] parts = surveyList.get(serverIdTemp).split("\\|"); //0 = Question, 1 = 1st Answer, 2 = 2nd Answer
        return new SurveyQuestion(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }

    public long getFirstVoteNumber() {
        return getFirstVotes().size();
    }

    public long getFirstVoteNumbers(byte vote) {
        return getFirstVotes().values().stream().filter(secondVote -> secondVote.getVote() == vote).count();
    }

    public List<SurveySecondVote> getSurveySecondVotesForUserId(long userId) {
        return getSecondVotes().values().stream()
                .filter(surveySecondVote -> surveySecondVote.getUserId() == userId)
                .collect(Collectors.toList());
    }

    public byte getWon() {
        long votesA = getFirstVoteNumbers((byte)0);
        long votesTotal = getFirstVoteNumber();
        long votesB = votesTotal - votesA;

        if (votesA > votesB) return 0;
        if (votesA < votesB) return 1;
        return 2;
    }

}
