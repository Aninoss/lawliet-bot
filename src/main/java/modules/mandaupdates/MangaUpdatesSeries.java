package modules.mandaupdates;

public class MangaUpdatesSeries {

    private final long seriesId;
    private final String title;
    private final String image;
    private final String url;
    private final boolean nsfw;

    public MangaUpdatesSeries(long seriesId, String title, String image, String url, boolean nsfw) {
        this.seriesId = seriesId;
        this.title = title;
        this.image = image;
        this.url = url;
        this.nsfw = nsfw;
    }

    public long getSeriesId() {
        return seriesId;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getUrl() {
        return url;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public String getReleasesUrl() {
        return "https://www.mangaupdates.com/releases.html?search=" + getSeriesId() + "&stype=series";
    }

}
