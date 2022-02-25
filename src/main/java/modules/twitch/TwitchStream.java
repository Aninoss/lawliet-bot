package modules.twitch;

public class TwitchStream {

    private boolean live;
    private String userId;
    private TwitchUser twitchUser;
    private String thumbnailUrl;
    private String game;
    private String title;
    private Integer viewers;

    public boolean isLive() {
        return live;
    }

    public TwitchStream setLive(boolean live) {
        this.live = live;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public TwitchStream setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public TwitchUser getTwitchUser() {
        return twitchUser;
    }

    public TwitchStream setTwitchUser(TwitchUser twitchUser) {
        this.twitchUser = twitchUser;
        return this;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public TwitchStream setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public String getGame() {
        return game;
    }

    public TwitchStream setGame(String game) {
        this.game = game;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public TwitchStream setTitle(String title) {
        this.title = title;
        return this;
    }

    public Integer getViewers() {
        return viewers;
    }

    public TwitchStream setViewers(Integer viewers) {
        this.viewers = viewers;
        return this;
    }

}
