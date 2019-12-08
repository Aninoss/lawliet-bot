package General.AnimeNews;

import java.time.Instant;

public class AnimeReleasePost {

    private String anime, description, episodeTitle, thumbnail, url;
    private int episodeNum, id;
    private Instant date;

    public AnimeReleasePost(String anime, String description, int episodeNum, String episodeTitle, String thumbnail, Instant date, String url, int id) {
        this.anime = anime;
        this.description = description;
        this.episodeNum = episodeNum;
        this.episodeTitle = episodeTitle;
        this.thumbnail = thumbnail;
        this.date = date;
        this.url = url;
        this.id = id;
    }

    public String getAnime() {
        return anime;
    }

    public int getEpisodeNum() {
        return episodeNum;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public Instant getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
