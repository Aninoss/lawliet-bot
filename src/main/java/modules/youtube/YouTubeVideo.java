package modules.youtube;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class YouTubeVideo {

    private final String creator;
    private final String creatorUrl;
    private final String title;
    private final String thumbnail;
    private final String link;
    private final Instant publicationTime;
    private final long views;
    private final Long likes;

    public YouTubeVideo(String creator, String creatorUrl, String title, String thumbnail, String link, Instant publicationTime, long views, Long likes) {
        this.creator = creator;
        this.creatorUrl = creatorUrl;
        this.title = title;
        this.thumbnail = thumbnail;
        this.link = link;
        this.publicationTime = publicationTime.truncatedTo(ChronoUnit.MINUTES);
        this.views = views;
        this.likes = likes;
    }

    public String getCreator() {
        return creator;
    }

    public String getCreatorUrl() {
        return creatorUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getLink() {
        return link;
    }

    public Instant getPublicationTime() {
        return publicationTime;
    }

    public long getViews() {
        return views;
    }

    public Long getLikes() {
        return likes;
    }

}