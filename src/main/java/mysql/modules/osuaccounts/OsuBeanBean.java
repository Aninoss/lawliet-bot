package mysql.modules.osuaccounts;

public class OsuBeanBean {

    private final long userId;
    private final long osuId;

    public OsuBeanBean(long userId, long osuId) {
        this.userId = userId;
        this.osuId = osuId;
    }

    public long getUserId() {
        return userId;
    }

    public long getOsuId() {
        return osuId;
    }

}
