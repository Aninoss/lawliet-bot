package Modules.Porn;

import java.time.Instant;

public class PornImage {

    private final String imageUrl, pageUrl;
    private final int score;
    private final Instant instant;
    private final boolean video;

    public PornImage(String imageUrl, String pageUrl, int score, Instant instant, boolean video) {
        this.imageUrl = imageUrl;
        this.pageUrl = pageUrl;
        this.score = score;
        this.instant = instant;
        this.video = video;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public int getScore() {
        return score;
    }

    public Instant getInstant() {
        return instant;
    }

    public boolean isVideo() {
        return video;
    }
}
