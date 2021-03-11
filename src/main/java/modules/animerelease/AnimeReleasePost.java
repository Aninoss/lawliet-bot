package modules.animerelease;

import java.time.Instant;
import java.util.Optional;

public class AnimeReleasePost {

    private final String anime, description, episodeTitle, thumbnail, url, episode;
    private final int id;
    private final Instant instant;

    public AnimeReleasePost(String anime, String description, String episode, String episodeTitle, String thumbnail, Instant instant, String url, int id) {
        this.anime = anime;
        this.description = description;
        this.episode = episode;
        this.episodeTitle = episodeTitle;
        this.thumbnail = thumbnail;
        this.instant = instant;
        this.url = url;
        this.id = id;
    }

    public String getAnime() {
        return anime;
    }

    public Optional<String> getEpisode() {
        return Optional.ofNullable(episode);
    }

    public Optional<String> getEpisodeTitle() {
        return Optional.ofNullable(episodeTitle);
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public Instant getInstant() {
        return instant;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

}
