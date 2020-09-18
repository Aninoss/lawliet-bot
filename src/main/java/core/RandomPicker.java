package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class RandomPicker {

    private static final RandomPicker ourInstance = new RandomPicker();
    public static RandomPicker getInstance() { return ourInstance; }
    private RandomPicker() {}

    private final HashMap<String, HashMap<Long, ArrayList<Integer>>> picks = new HashMap<>();

    public int pick(String tag, long serverId, int size) {
        HashMap<Long, ArrayList<Integer>> tagPicks = picks.computeIfAbsent(tag, k -> new HashMap<>());
        ArrayList<Integer> serverPicks = tagPicks.computeIfAbsent(serverId, k -> new ArrayList<>());

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

}
