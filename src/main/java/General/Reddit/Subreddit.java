package General.Reddit;

import java.util.ArrayList;
import java.util.Random;

public class Subreddit {
    private String name, postReference, tempReference;
    private ArrayList<Integer> used;
    private int limit = 25;

    public Subreddit(String name) {
        this.name = name;
        postReference = "";
        tempReference = postReference;
        used = new ArrayList<>();
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

    public ArrayList<Integer> getUsed() {
        return used;
    }
}