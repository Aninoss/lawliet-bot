package modules.reddit;

import java.time.Instant;
import java.util.List;

public class RedditPost {

    private String id;
    private String title;
    private String author;
    private String redditUrl;
    private String contentUrl;
    private String subreddit;
    private List<String> mediaUrls;
    private String thumbnail;
    private String description;
    private String flair;
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

    public String getRedditUrl() {
        return redditUrl;
    }

    public RedditPost setRedditUrl(String redditUrl) {
        this.redditUrl = redditUrl;
        return this;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public RedditPost setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public RedditPost setSubreddit(String subreddit) {
        this.subreddit = subreddit;
        return this;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public RedditPost setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
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
