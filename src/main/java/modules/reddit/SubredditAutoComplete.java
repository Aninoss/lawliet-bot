package modules.reddit;

public class SubredditAutoComplete {

    private String name;
    private long subscribers;
    private boolean nsfw;

    public String getName() {
        return name;
    }

    public SubredditAutoComplete setName(String name) {
        this.name = name;
        return this;
    }

    public long getSubscribers() {
        return subscribers;
    }

    public SubredditAutoComplete setSubscribers(long subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public SubredditAutoComplete setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }

}
