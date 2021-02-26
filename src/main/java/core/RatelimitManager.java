package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.utils.TimeUtil;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

public class RatelimitManager {

    private static final RatelimitManager ourInstance = new RatelimitManager();

    public static RatelimitManager getInstance() {
        return ourInstance;
    }

    private RatelimitManager() {
    }

    private final Cache<String, ArrayList<Instant>> eventCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    /*
    @return the remaining amount of seconds
     */
    public Optional<Integer> checkAndSet(String type, Object key, int cap, int capTimeAmount, ChronoUnit capTimeUnit) {
        String stringKey = type + ":" + key;

        ArrayList<Instant> events = eventCache.asMap().computeIfAbsent(stringKey, k -> new ArrayList<>());
        synchronized (events) {
            if (events.size() >= cap) {
                Instant firstOccurence = events.get(0);
                long milisAgo = TimeUtil.getMilisBetweenInstants(firstOccurence, Instant.now());
                long milisCap = Duration.of(capTimeAmount, capTimeUnit).toMillis();

                if (milisAgo < milisCap) {
                    return Optional.of((int) Math.ceil((milisCap - milisAgo) / 1000.0));
                } else {
                    events.remove(0);
                }
            }

            events.add(Instant.now());
            return Optional.empty();
        }
    }

}
