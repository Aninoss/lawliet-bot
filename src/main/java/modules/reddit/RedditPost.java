package modules.reddit;

import java.time.Instant;

public class RedditPost {

    private String id;
    private String title;
    private String author;
    private String url;
    private String subreddit;
    private String domain;
    private String image;
    private String thumbnail;
    private String description;
    private String flair;
    private String sourceLink;
    private int score;
    private int comments;
    private boolean nsfw;
    private Instant instant;

    public RedditPost() {
    }

    public String getId() {
        return id;
    }

    public RedditPost setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public RedditPost setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public RedditPost setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public RedditPost setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public RedditPost setSubreddit(String subreddit) {
        this.subreddit = subreddit;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public RedditPost setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getImage() {
        return image;
    }

    public RedditPost setImage(String image) {
        this.image = image;
        return this;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public RedditPost setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public RedditPost setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getFlair() {
        return flair;
    }

    public RedditPost setFlair(String flair) {
        this.flair = flair;
        return this;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public RedditPost setSourceLink(String sourceLink) {
        this.sourceLink = sourceLink;
        return this;
    }

    public int getScore() {
        return score;
    }

    public RedditPost setScore(int score) {
        this.score = score;
        return this;
    }

    public int getComments() {
        return comments;
    }

    public RedditPost setComments(int comments) {
        this.comments = comments;
        return this;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public RedditPost setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }

    public Instant getInstant() {
        return instant;
    }

    public RedditPost setInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

}
