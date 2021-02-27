package modules.reddit;

import java.util.ArrayList;
import java.util.Random;

public class Subreddit {

    private final String name;
    private String postReference = "";
    private String tempReference = postReference;
    private ArrayList<Integer> used = new ArrayList<>();
    private int limit = 25;

    public Subreddit(String name) {
        this.name = name;
    }

    public int getRemainingIndex(String postReference, int limit) {
        tempReference = postReference;
        this.limit = limit;

        int n;
        Random r = new Random();
        do {
            n = r.nextInt(limit);
        } while (used.contains(n));

        used.add(n);
        if (used.size() >= limit) {
            used.remove(0);
        }

        return n;
    }

    public String getName() {
        return name;
    }

    public String getPostReference() {
        if (used.size() >= limit) {
            this.postReference = tempReference;
            used = new ArrayList<>();
        }
        return postReference;
    }

}