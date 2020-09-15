package Modules.Twitch;

public class TwitchUser {

    private final String channelId;
    private final String channelName;
    private final String logoUrl;
    private final String displayName;

    public TwitchUser(String channelId, String channelName, String displayName, String logoUrl) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.displayName = displayName;
        this.logoUrl = logoUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getChannelUrl() {
        return "https://www.twitch.tv/" + channelName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

}
