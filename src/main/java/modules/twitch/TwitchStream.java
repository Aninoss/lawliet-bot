package modules.twitch;

import java.util.Optional;

public class TwitchStream {

    private final boolean live;
    private final TwitchUser twitchUser;
    private final String previewImage;
    private final String game;
    private final String status;
    private final Integer viewers;
    private final Integer followers;

    public TwitchStream(TwitchUser twitchUser) {
        this.live = false;
        this.twitchUser = twitchUser;
        this.previewImage = null;
        this.game = null;
        this.status = null;
        this.viewers = null;
        this.followers = null;
    }

    public TwitchStream(TwitchUser twitchUser, String previewImage, String game, String status, int viewers, int followers) {
        this.live = true;
        this.twitchUser = twitchUser;
        this.previewImage = previewImage;
        this.game = game;
        this.status = status;
        this.viewers = viewers;
        this.followers = followers;
    }

    public Optional<String> getPreviewImage() {
        return Optional.ofNullable(previewImage);
    }

    public Optional<String> getGame() {
        return Optional.ofNullable(game);
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public TwitchUser getTwitchUser() {
        return twitchUser;
    }

    public Optional<Integer> getViewers() {
        return Optional.ofNullable(viewers);
    }

    public Optional<Integer> getFollowers() {
        return Optional.ofNullable(followers);
    }

    public boolean isLive() {
        return live;
    }

}
