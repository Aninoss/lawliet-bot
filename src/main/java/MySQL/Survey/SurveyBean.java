package MySQL.Survey;

import General.CustomObservableMap;
import javafx.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Observable;

public class SurveyBean extends Observable {

    private int surveyId;
    private LocalDate startDate;
    private CustomObservableMap<Long, SurveyFirstVote> firstVotes;
    private CustomObservableMap<Pair<Long, Long>, SurveySecondVote> secondVotes; /* Pair: serverId, userId */

    public SurveyBean(int surveyId, LocalDate startDate, @NonNull HashMap<Long, SurveyFirstVote> firstVotes, @NonNull HashMap<Pair<Long, Long>, SurveySecondVote> secondVotes) {
        this.surveyId = surveyId;
        this.startDate = startDate;
        this.firstVotes = new CustomObservableMap<>(firstVotes);
        this.secondVotes = new CustomObservableMap<>(secondVotes);
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

}
