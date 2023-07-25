package modules.pixiv;

import java.time.Instant;
import java.util.List;

public class PixivImage {

    private String id;
    private String title;
    private String description;
    private String author;
    private String authorUrl;
    private String url;
    private List<String> imageUrls;
    private int views;
    private int bookmarks;
    private boolean nsfw;
    private Instant instant;

    public String getId() {
        return id;
    }

    public PixivImage setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public PixivImage setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PixivImage setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public PixivImage setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public PixivImage setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PixivImage setUrl(String url) {
        this.url = url;
        return this;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public PixivImage setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        return this;
    }

    public int getViews() {
        return views;
    }

    public PixivImage setViews(int views) {
        this.views = views;
        return this;
    }

    public int getBookmarks() {
        return bookmarks;
    }

    public PixivImage setBookmarks(int bookmarks) {
        this.bookmarks = bookmarks;
        return this;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public PixivImage setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }

    public Instant getInstant() {
        return instant;
    }

    public PixivImage setInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

}
