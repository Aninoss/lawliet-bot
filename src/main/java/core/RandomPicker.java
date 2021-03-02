package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class RandomPicker {

    private static final RandomPicker ourInstance = new RandomPicker();

    public static RandomPicker getInstance() {
        return ourInstance;
    }

    private RandomPicker() {
    }

    private final HashMap<String, Cache<Long, ArrayList<Integer>>> picks = new HashMap<>();

    public synchronized int pick(String tag, long serverId, int size) {
        Cache<Long, ArrayList<Integer>> tagPicks = picks.computeIfAbsent(tag, k -> generateCache());
        ArrayList<Integer> serverPicks = tagPicks.asMap().computeIfAbsent(serverId, k -> new ArrayList<>());

        Random n = new Random();
        int i;
        do {
            i = n.nextInt(size);
        } while (serverPicks.contains(i));
        serverPicks.add(i);
        if (serverPicks.size() == size)
            serverPicks.remove(0);

        return i;
    }

    private Cache<Long, ArrayList<Integer>> generateCache() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(30))
                .build();
    }

}
