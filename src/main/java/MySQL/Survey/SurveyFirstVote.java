package MySQL.Survey;

public class SurveyFirstVote {

    private long userId;
    private byte vote;

    public SurveyFirstVote(long userId, byte vote) {
        this.userId = userId;
        this.vote = vote;
    }

    public long getUserId() {
        return userId;
    }

    public byte getVote() {
        return vote;
    }

}
