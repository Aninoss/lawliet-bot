package modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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

    public static <T> PostBundle<T> create(List<T> posts, String args, Function<T, String> idFunction) {
        ArrayList<T> newPosts = new ArrayList<>();
        ArrayList<String> usedIdList = new ArrayList<>();
        if (args != null) {
            usedIdList.addAll(Arrays.asList(args.split("\\|")));
        }

        for (T post : posts) {
            String id = idFunction.apply(post);
            if (!usedIdList.contains(id)) {
                newPosts.add(post);
                usedIdList.add(id);
            }
        }

        while (usedIdList.size() > 100) {
            usedIdList.remove(0);
        }

        StringBuilder newArg = new StringBuilder();
        for (int i = 0; i < usedIdList.size(); i++) {
            if (i > 0) {
                newArg.append("|");
            }
            newArg.append(usedIdList.get(i));
        }

        return new PostBundle<>(newPosts, newArg.toString());
    }

}