package core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.TimeUtil;

public class RatelimitManager {

    private static final RatelimitManager ourInstance = new RatelimitManager();

    public static RatelimitManager getInstance() {
        return ourInstance;
    }

    private RatelimitManager() {
    }

    private final Cache<String, ArrayList<Instant>> eventCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    /*
    @return the remaining amount of seconds
     */
    public synchronized Optional<Integer> checkAndSet(String type, Object key, int cap, Duration duration) {
        String stringKey = type + ":" + key;

        ArrayList<Instant> events = eventCache.asMap().computeIfAbsent(stringKey, k -> new ArrayList<>());
        if (events.size() >= cap) {
            Instant firstOccurence = events.get(0);
            long millisAgo = TimeUtil.getMillisBetweenInstants(firstOccurence, Instant.now());
            long millisCap = duration.toMillis();

            if (millisAgo < millisCap) {
                return Optional.of((int) Math.ceil((millisCap - millisAgo) / 1000.0));
            } else {
                events.remove(0);
            }
        }

        events.add(Instant.now());
        return Optional.empty();
    }

}
