package MySQL.Survey;

public class SurveySecondVote {

    private long serverId, userId;
    private byte vote;

    public SurveySecondVote(long serverId, long userId, byte vote) {
        this.serverId = serverId;
        this.userId = userId;
        this.vote = vote;
    }

    public long getServerId() {
        return serverId;
    }

    public long getUserId() {
        return userId;
    }

    public byte getVote() {
        return vote;
    }

}
