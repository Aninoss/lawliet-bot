package modules.anilist;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class AnilistMedia {

    public enum Status { NOT_YET_RELEASED, RELEASING, FINISHED, CANCELLED }

    private final int id;
    private final String title;
    private final String description;
    private final String coverImage;
    private final String anilistUrl;
    private final Status status;
    private final boolean isAdult;
    private final List<String> genres;
    private final Integer totalEpisodes;
    private final Integer currentEpisode;
    private final Instant nextEpisode;
    private final int averageScore;

    public AnilistMedia(int id, String title, String description, String coverImage, String anilistUrl, Status status,
                        boolean isAdult, List<String> genres, Integer totalEpisodes, Integer currentEpisode, Instant nextEpisode,
                        int averageScore
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.coverImage = coverImage;
        this.anilistUrl = anilistUrl;
        this.status = status;
        this.isAdult = isAdult;
        this.genres = genres;
        this.totalEpisodes = totalEpisodes;
        this.currentEpisode = currentEpisode;
        this.nextEpisode = nextEpisode;
        this.averageScore = averageScore;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getAnilistUrl() {
        return anilistUrl;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isAdult() {
        return isAdult;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Integer getTotalEpisodes() {
        return totalEpisodes;
    }

    public Integer getCurrentEpisode() {
        return currentEpisode;
    }

    public Instant getNextEpisode() {
        return nextEpisode;
    }

    public int getAverageScore() {
        return averageScore;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status.name(), currentEpisode);
    }

}
