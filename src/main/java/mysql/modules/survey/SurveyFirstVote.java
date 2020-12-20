package mysql.modules.survey;

import java.util.Locale;

public class SurveyFirstVote {

    private final long userId;
    private final byte vote;
    private final Locale locale;

    public SurveyFirstVote(long userId, byte vote, Locale locale) {
        this.userId = userId;
        this.vote = vote;
        this.locale = locale;
    }

    public long getUserId() {
        return userId;
    }

    public byte getVote() {
        return vote;
    }

    public Locale getLocale() {
        return locale;
    }

}
