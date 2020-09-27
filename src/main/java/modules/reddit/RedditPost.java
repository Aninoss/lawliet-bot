package modules.reddit;

import java.time.Instant;

public class RedditPost {

    private String title, author, url, subreddit, domain, image, thumbnail, description, flair;
    private int score, comments;
    private boolean nsfw;
    private Instant instant;

    public RedditPost() {}

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUrl(String url) {
        if (url.startsWith("/"))
            url = "https://www.reddit.com" + url;
        this.url = url;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public String getDomain() {
        return domain;
    }

    public int getScore() {
        return score;
    }

    public int getComments() {
        return comments;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFlair() {
        return flair;
    }

    public void setFlair(String flair) {
        this.flair = flair;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }
}
