package Modules;

import java.util.List;

public class PostBundle<T> {
    private List<T> posts;
    private String newestPost;

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