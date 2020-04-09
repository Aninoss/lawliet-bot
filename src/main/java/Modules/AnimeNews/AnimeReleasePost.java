package Modules.AnimeNews;

import java.time.Instant;
import java.util.Optional;

public class AnimeReleasePost {

    private String anime, description, episodeTitle, thumbnail, url, episode;
    private int id;
    private Instant date;

    public AnimeReleasePost(String anime, String description, String episode, String episodeTitle, String thumbnail, Instant date, String url, int id) {
        this.anime = anime;
        this.description = description;
        this.episode = episode;
        this.episodeTitle = episodeTitle;
        this.thumbnail = thumbnail;
        this.date = date;
        this.url = url;
        this.id = id;
    }

    public String getAnime() {
        return anime;
    }

    public Optional<String> getEpisode() {
        return Optional.ofNullable(episode);
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
