package modules.animenews;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AnimeNewsArticle {

    private final String title;
    private final String description;
    private final String thumbnail;
    private final String link;
    private final Instant publicationTime;

    public AnimeNewsArticle(String title, String description, String thumbnail, String link, Instant publicationTime) {
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.link = link;
        this.publicationTime = publicationTime.truncatedTo(ChronoUnit.MINUTES);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

}