package mysql.modules.survey;

public class SurveyFirstVote {

    private final long userId;
    private final byte vote;

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
