package modules.porn;

import java.time.Instant;
import java.util.List;

public class BooruImage {

    private long id;
    private String imageUrl;
    private String originalImageUrl;
    private String pageUrl;
    private int score;
    private Instant instant;
    private boolean video;
    private List<String> tags;
    private List<String> imageTags;

    public long getId() {
        return id;
    }

    public BooruImage setId(long id) {
        this.id = id;
        return this;

    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BooruImage setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public BooruImage setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
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

    public boolean getVideo() {
        return video;
    }

    public BooruImage setVideo(boolean video) {
        this.video = video;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public BooruImage setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public boolean isVideo() {
        return video;
    }

    public List<String> getImageTags() {
        return imageTags;
    }

    public BooruImage setImageTags(List<String> imageTags) {
        this.imageTags = imageTags;
        return this;
    }

}
