package core;

import core.utils.TimeUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class SpamChecker {

    private static final SpamChecker ourInstance = new SpamChecker();
    public static SpamChecker getInstance() { return ourInstance; }
    private SpamChecker() { }

    private HashMap<String, HashMap<Object, ArrayList<Instant>>> typeMap = new HashMap<>();

    public boolean checkAndSet(String type, Object key, int cap, int capTimeAmount, ChronoUnit capTimeUnit) {
        ArrayList<Instant> events = typeMap.computeIfAbsent(type, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>());

        synchronized (events) {
            if (events.size() >= cap) {
                Instant firstOccurence = events.get(0);
                long milisAgo = TimeUtil.getMilisBetweenInstants(firstOccurence, Instant.now());
                long milisCap = Duration.of(capTimeAmount, capTimeUnit).toMillis();

                if (milisAgo < milisCap)
                    return false;
                else
                    events.remove(0);
            }

            events.add(Instant.now());
            return true;
        }
    }

}
