package mysql.modules.survey;

import core.assets.UserAsset;

import java.util.Locale;

public class SurveyFirstVote implements UserAsset {

    private final long userId;
    private final byte vote;
    private final Locale locale;

    public SurveyFirstVote(long userId, byte vote, Locale locale) {
        this.userId = userId;
        this.vote = vote;
        this.locale = locale;
    }

    @Override
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
