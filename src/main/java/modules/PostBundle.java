package modules;

import java.util.List;

public class PostBundle<T> {
    private final List<T> posts;
    private final String newestPost;

    public PostBundle(List<T> posts, String newestPost) {
        this.posts = posts;
        this.newestPost = newestPost;
    }

    public List<T> getPosts() {
        return posts;
    }

    public String getNewestPost() {
        return newestPost;
    }
}