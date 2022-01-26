package mysql.modules.userprivatechannels;

public class PrivateChannelData {

    private final long userId;
    private final long privateChannelId;

    public PrivateChannelData(long userId, long privateChannelId) {
        this.userId = userId;
        this.privateChannelId = privateChannelId;
    }

    public long getUserId() {
        return userId;
    }

    public long getPrivateChannelId() {
        return privateChannelId;
    }

}
