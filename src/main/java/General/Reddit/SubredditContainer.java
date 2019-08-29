package General.Reddit;

import java.util.ArrayList;

public class SubredditContainer {
    private static SubredditContainer ourInstance = new SubredditContainer();
    private ArrayList<Subreddit> subreddits;

    public static SubredditContainer getInstance() {
        return ourInstance;
    }

    private SubredditContainer() {
        subreddits = new ArrayList<>();
    }

    public Subreddit get(String name) {
        for(Subreddit subreddit: subreddits) {
            if (subreddit.getName().equalsIgnoreCase(name)) return subreddit;
        }

        Subreddit subreddit = new Subreddit(name);
        subreddits.add(subreddit);

        return subreddit;
    }

    public void reset() {
        subreddits = new ArrayList<>();
    }
}