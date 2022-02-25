package modules.twitch;

public class TwitchUser {

    private String userId;
    private String login;
    private String profileImageUrl;
    private String displayName;

    public String getUserId() {
        return userId;
    }

    public TwitchUser setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public TwitchUser setLogin(String login) {
        this.login = login;
        return this;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public TwitchUser setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TwitchUser setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

}
