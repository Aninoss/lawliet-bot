package core;

import core.utils.TimeUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class RatelimitManager {

    private static final RatelimitManager ourInstance = new RatelimitManager();
    public static RatelimitManager getInstance() { return ourInstance; }
    private RatelimitManager() { }

    private HashMap<String,ArrayList<Instant>> eventMap = new HashMap<>();

    public boolean checkAndSet(String type, Object key, int cap, int capTimeAmount, ChronoUnit capTimeUnit) {
        String stringKey = type + ":" + key;

        ArrayList<Instant> events = eventMap.computeIfAbsent(stringKey, k -> new ArrayList<>());
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
