package modules.porn;

import java.time.Instant;

public class BooruImage {

    private String imageUrl;
    private String pageUrl;
    private int score;
    private Instant instant;
    private boolean video;

    public String getImageUrl() {
        return imageUrl;
    }

    public BooruImage setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public BooruImage setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
        return this;
    }

    public int getScore() {
        return score;
    }

    public BooruImage setScore(int score) {
        this.score = score;
        return this;
    }

    public Instant getInstant() {
        return instant;
    }

    public BooruImage setInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    public boolean isVideo() {
        return video;
    }

    public BooruImage setVideo(boolean video) {
        this.video = video;
        return this;
    }

}
