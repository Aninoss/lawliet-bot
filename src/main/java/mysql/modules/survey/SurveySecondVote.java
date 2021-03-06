package mysql.modules.survey;

import core.assets.MemberAsset;

public class SurveySecondVote implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private final byte vote;

    public SurveySecondVote(long guildId, long memberId, byte vote) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.vote = vote;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public byte getVote() {
        return vote;
    }

}
