package Modules.Porn;

import java.time.Instant;

public class PornImage {

    private String imageUrl, pageUrl;
    private int score, comments;
    private Instant instant;
    private boolean video;

    public PornImage(String imageUrl, String pageUrl, int score, int comments, Instant instant, boolean video) {
        this.imageUrl = imageUrl;
        this.pageUrl = pageUrl;
        this.score = score;
        this.comments = comments;
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

    public int getComments() { return comments; }

    public long getWeight() {
        return (long) Math.pow(score + 1, 2.75) * (imageUrl.endsWith("gif") ? 3 : 1);
    }

    public Instant getInstant() {
        return instant;
    }

    public boolean isVideo() {
        return video;
    }
}
